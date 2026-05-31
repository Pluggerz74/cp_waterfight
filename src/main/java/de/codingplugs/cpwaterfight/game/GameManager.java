package de.codingplugs.cpwaterfight.game;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.display.JoinDisplayManager;
import de.codingplugs.cpwaterfight.join.ArenaPlayerCountProvider;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages per-arena game sessions, countdown, and match start flow.
 */
public final class GameManager {

    private static final String COUNTDOWN_SECONDS_PATH = "game.countdown-seconds";
    private static final String PREVENT_FALL_DAMAGE_PATH = "game.prevent-fall-damage-before-start";
    private static final Set<Integer> MILESTONE_SECONDS = Set.of(60, 30, 10);

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messages;
    private final JoinDisplayManager joinDisplayManager;
    private ArenaPlayerCountProvider playerCountProvider = arena -> 0;
    private JoinManager joinManager;

    private final Map<String, GameSession> sessions = new HashMap<>();

    public GameManager(
            JavaPlugin plugin,
            ConfigManager configManager,
            MessageManager messages,
            JoinDisplayManager joinDisplayManager
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messages = messages;
        this.joinDisplayManager = joinDisplayManager;
    }

    public void setPlayerCountProvider(ArenaPlayerCountProvider playerCountProvider) {
        if (playerCountProvider != null) {
            this.playerCountProvider = playerCountProvider;
        }
    }

    public void setJoinManager(JoinManager joinManager) {
        this.joinManager = joinManager;
    }

    public void load() {
        sessions.clear();
    }

    public void reload() {
        stopAll();
    }

    public void shutdown() {
        stopAll();
    }

    public void stopAll() {
        for (GameSession session : sessions.values()) {
            session.reset();
        }
        sessions.clear();
    }

    public boolean canJoin(Arena arena) {
        if (arena == null) {
            return false;
        }

        GameState state = getArenaState(arena);
        return state != GameState.INGAME && state != GameState.ENDING;
    }

    public boolean isFallDamageProtectionEnabled() {
        return configManager.config().getBoolean(PREVENT_FALL_DAMAGE_PATH, true);
    }

    public Optional<GameSession> getSession(Arena arena) {
        if (arena == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(arena.id()));
    }

    public GameSession getOrCreateSession(Arena arena) {
        if (arena == null) {
            throw new IllegalArgumentException("arena must not be null");
        }
        return sessions.computeIfAbsent(arena.id(), GameSession::new);
    }

    public void handlePlayerJoin(Player player, Arena arena) {
        if (player == null || arena == null) {
            return;
        }

        GameSession session = getOrCreateSession(arena);
        session.addPlayer(player.getUniqueId());

        GameState state = session.getState();
        if (state == GameState.WAITING && getOnlinePlayers(arena).size() >= arena.minPlayers()) {
            startCountdown(arena);
        }

        refreshDisplay(arena);
    }

    public void handlePlayerLeave(Player player, Arena arena) {
        if (player == null || arena == null) {
            return;
        }

        GameSession session = getSession(arena).orElse(null);
        if (session == null) {
            return;
        }

        session.removePlayer(player.getUniqueId());

        if (session.getState() == GameState.COUNTDOWN
                && getOnlinePlayers(arena).size() < arena.minPlayers()) {
            cancelCountdown(arena);
            session.setState(GameState.WAITING);
            broadcast(arena, "game.countdown-cancelled", placeholders(arena));
        }

        refreshDisplay(arena);
    }

    public void handleQuit(Player player) {
        if (player == null || joinManager == null) {
            return;
        }
        joinManager.leave(player, false);
    }

    public void handleDeath(Player player) {
        // Respawn location is applied in PlayerRespawnEvent when the player is in-game.
    }

    public GameActionResult forceStart(Arena arena) {
        if (arena == null) {
            return GameActionResult.NOT_RUNNING;
        }

        GameSession session = getOrCreateSession(arena);
        GameState state = session.getState();

        if (state == GameState.INGAME || state == GameState.ENDING) {
            return GameActionResult.ALREADY_RUNNING;
        }

        if (playerCountProvider.getPlayerCount(arena) < 1) {
            return GameActionResult.NO_PLAYERS;
        }

        if (arena.spawnCount() == 0) {
            return GameActionResult.MISSING_SPAWNS;
        }

        cancelCountdown(arena);
        startGame(arena, true);
        return GameActionResult.SUCCESS;
    }

    public GameActionResult stopGame(Arena arena) {
        if (arena == null) {
            return GameActionResult.NOT_RUNNING;
        }

        GameSession session = getSession(arena).orElse(null);
        if (session == null) {
            return GameActionResult.NOT_RUNNING;
        }

        GameState state = session.getState();
        if (state == GameState.WAITING && !session.isCountdownRunning()) {
            return GameActionResult.NOT_RUNNING;
        }

        cancelCountdown(arena);
        session.setState(GameState.ENDING);
        refreshDisplay(arena);

        teleportPlayersToLobby(arena);

        session.setState(GameState.WAITING);
        broadcast(arena, "game.stopped", placeholders(arena));
        refreshDisplay(arena);
        return GameActionResult.SUCCESS;
    }

    public void startCountdown(Arena arena) {
        if (arena == null) {
            return;
        }

        GameSession session = getOrCreateSession(arena);
        if (session.getState() != GameState.WAITING || session.isCountdownRunning()) {
            return;
        }

        if (getOnlinePlayers(arena).size() < arena.minPlayers()) {
            return;
        }

        int seconds = countdownSeconds();
        session.setState(GameState.COUNTDOWN);
        session.setCountdownSecondsRemaining(seconds);
        broadcast(arena, "game.countdown-started", placeholders(arena, seconds));

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> tickCountdown(arena),
                20L,
                20L
        );
        session.setCountdownTask(task);
        refreshDisplay(arena);
    }

    public void cancelCountdown(Arena arena) {
        if (arena == null) {
            return;
        }

        getSession(arena).ifPresent(session -> {
            session.cancelCountdown();
            session.resetCountdown();
            if (session.getState() == GameState.COUNTDOWN) {
                session.setState(GameState.WAITING);
            }
        });
        refreshDisplay(arena);
    }

    public void startGame(Arena arena) {
        startGame(arena, false);
    }

    public void startGame(Arena arena, boolean force) {
        if (arena == null) {
            return;
        }

        GameSession session = getOrCreateSession(arena);
        session.cancelCountdown();
        session.resetCountdown();

        List<Player> onlinePlayers = getOnlinePlayers(arena);

        if (!force && onlinePlayers.size() < arena.minPlayers()) {
            session.setState(GameState.WAITING);
            broadcast(arena, "game.not-enough-players", placeholders(arena));
            refreshDisplay(arena);
            return;
        }

        if (force && onlinePlayers.isEmpty()) {
            session.setState(GameState.WAITING);
            return;
        }

        if (arena.spawnCount() == 0) {
            session.setState(GameState.WAITING);
            broadcast(arena, "game.missing-spawns", placeholders(arena));
            refreshDisplay(arena);
            return;
        }

        session.setState(GameState.INGAME);

        for (Player player : onlinePlayers) {
            teleportToRandomSpawn(player, arena);
        }

        broadcast(arena, "game.started", placeholders(arena));
        refreshDisplay(arena);
    }

    public boolean canStart(Arena arena) {
        if (arena == null) {
            return false;
        }
        return getOnlinePlayers(arena).size() >= arena.minPlayers() && arena.spawnCount() > 0;
    }

    public List<Player> getOnlinePlayers(Arena arena) {
        List<Player> online = new ArrayList<>();
        if (arena == null) {
            return online;
        }

        GameSession session = sessions.get(arena.id());
        if (session == null) {
            return online;
        }

        for (UUID playerId : session.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                online.add(player);
            }
        }
        return online;
    }

    public Optional<Location> getRandomSpawn(Arena arena) {
        if (arena == null || arena.spawnCount() == 0) {
            return Optional.empty();
        }

        List<Location> spawns = arena.spawns();
        Location spawn = spawns.get(ThreadLocalRandom.current().nextInt(spawns.size()));
        if (spawn == null || spawn.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.of(spawn.clone());
    }

    public void teleportToRandomSpawn(Player player, Arena arena) {
        getRandomSpawn(arena).ifPresent(player::teleport);
    }

    public void broadcast(Arena arena, String messagePath, Map<String, String> placeholders) {
        for (Player player : getOnlinePlayers(arena)) {
            messages.sendPrefixed(player, messagePath, placeholders);
        }
    }

    public GameState getArenaState(Arena arena) {
        if (arena == null) {
            return GameState.WAITING;
        }
        return getSession(arena).map(GameSession::getState).orElse(GameState.WAITING);
    }

    public int getPlayerCount(Arena arena) {
        if (arena == null) {
            return 0;
        }
        return getSession(arena).map(GameSession::getPlayerCount).orElse(0);
    }

    public boolean isInGame(Player player) {
        if (player == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        for (GameSession session : sessions.values()) {
            if (session.getState() == GameState.INGAME && session.hasPlayer(playerId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInWaterFight(Player player) {
        if (player == null || joinManager == null) {
            return false;
        }
        return joinManager.isInArena(player);
    }

    public Optional<Arena> getArena(Player player) {
        if (joinManager == null) {
            return Optional.empty();
        }
        return joinManager.getArena(player);
    }

    public void removeSession(String arenaId) {
        if (arenaId == null) {
            return;
        }

        GameSession session = sessions.remove(arenaId);
        if (session != null) {
            session.reset();
        }
    }

    private void tickCountdown(Arena arena) {
        GameSession session = getSession(arena).orElse(null);
        if (session == null || session.getState() != GameState.COUNTDOWN) {
            cancelCountdown(arena);
            return;
        }

        if (getOnlinePlayers(arena).size() < arena.minPlayers()) {
            cancelCountdown(arena);
            session.setState(GameState.WAITING);
            broadcast(arena, "game.countdown-cancelled", placeholders(arena));
            refreshDisplay(arena);
            return;
        }

        int remaining = session.getCountdownSecondsRemaining();
        if (remaining <= 0) {
            session.cancelCountdown();
            session.resetCountdown();
            startGame(arena);
            return;
        }

        if (shouldBroadcastTick(remaining)) {
            broadcast(arena, "game.countdown-tick", placeholders(arena, remaining));
        }

        session.setCountdownSecondsRemaining(remaining - 1);
    }

    private void teleportPlayersToLobby(Arena arena) {
        if (!arena.hasLobby()) {
            return;
        }

        Location lobby = arena.lobby();
        if (lobby == null || lobby.getWorld() == null) {
            return;
        }

        for (Player player : getOnlinePlayers(arena)) {
            player.teleport(lobby);
        }
    }

    private boolean shouldBroadcastTick(int remainingSeconds) {
        return remainingSeconds <= 5 || MILESTONE_SECONDS.contains(remainingSeconds);
    }

    private int countdownSeconds() {
        int configured = configManager.config().getInt(COUNTDOWN_SECONDS_PATH, -1);
        if (configured > 0) {
            return configured;
        }
        return Math.max(1, configManager.config().getInt("countdown-seconds", 10));
    }

    private Map<String, String> placeholders(Arena arena) {
        return Map.of(
                "arena", arena.displayName(),
                "players", String.valueOf(playerCountProvider.getPlayerCount(arena)),
                "min_players", String.valueOf(arena.minPlayers())
        );
    }

    private Map<String, String> placeholders(Arena arena, int seconds) {
        Map<String, String> values = new HashMap<>(placeholders(arena));
        values.put("seconds", String.valueOf(seconds));
        return values;
    }

    private void refreshDisplay(Arena arena) {
        joinDisplayManager.updateArena(arena, playerCountProvider.getPlayerCount(arena));
    }
}
