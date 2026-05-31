package de.codingplugs.cpwaterfight.game;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.display.JoinDisplayManager;
import de.codingplugs.cpwaterfight.join.ArenaPlayerCountProvider;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.scoreboard.ScoreboardManager;
import de.codingplugs.cpwaterfight.level.LevelDefinition;
import de.codingplugs.cpwaterfight.level.LevelManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import de.codingplugs.cpwaterfight.spectator.SpectatorManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
    private static final String CLEAR_INVENTORY_ON_STOP_PATH = "game.clear-inventory-on-stop";
    private static final String CLEAR_INVENTORY_ON_LEAVE_PATH = "game.clear-inventory-on-leave";
    private static final String ENDING_SECONDS_PATH = "game.ending-seconds";
    private static final String HIDE_DEATH_MESSAGES_PATH = "game.hide-death-messages";
    private static final String BROADCAST_LEVEL_UP_PATH = "game.broadcast-level-up";
    private static final Set<Integer> MILESTONE_SECONDS = Set.of(60, 30, 10);

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messages;
    private final JoinDisplayManager joinDisplayManager;
    private final LevelManager levelManager;
    private ArenaPlayerCountProvider playerCountProvider = arena -> 0;
    private JoinManager joinManager;
    private ScoreboardManager scoreboardManager;
    private SpectatorManager spectatorManager;

    private final Map<String, GameSession> sessions = new HashMap<>();

    public GameManager(
            JavaPlugin plugin,
            ConfigManager configManager,
            MessageManager messages,
            JoinDisplayManager joinDisplayManager,
            LevelManager levelManager
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messages = messages;
        this.joinDisplayManager = joinDisplayManager;
        this.levelManager = levelManager;
    }

    public void setPlayerCountProvider(ArenaPlayerCountProvider playerCountProvider) {
        if (playerCountProvider != null) {
            this.playerCountProvider = playerCountProvider;
        }
    }

    public void setJoinManager(JoinManager joinManager) {
        this.joinManager = joinManager;
    }

    public void setScoreboardManager(ScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    public void setSpectatorManager(SpectatorManager spectatorManager) {
        this.spectatorManager = spectatorManager;
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

    public boolean shouldHideDeathMessages() {
        return configManager.config().getBoolean(HIDE_DEATH_MESSAGES_PATH, true);
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
        refreshScoreboards(arena);
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

        if (!isSessionEmpty(arena)
                && session.getState() == GameState.COUNTDOWN
                && getOnlinePlayers(arena).size() < arena.minPlayers()) {
            cancelCountdown(arena);
            session.setState(GameState.WAITING);
            broadcast(arena, "game.countdown-cancelled", placeholders(arena));
        }

        refreshDisplay(arena);
        refreshScoreboards(arena);
    }

    public boolean isSessionEmpty(Arena arena) {
        return arena != null && playerCountProvider.getPlayerCount(arena) == 0;
    }

    public void handleArenaEmptyIfNeeded(Arena arena) {
        if (!isSessionEmpty(arena)) {
            return;
        }
        resetEmptySession(arena);
    }

    public void resetEmptySession(Arena arena) {
        if (arena == null) {
            return;
        }

        GameSession session = getSession(arena).orElse(null);
        if (session == null) {
            refreshDisplay(arena);
            return;
        }

        GameState previousState = session.getState();
        boolean hadActiveRuntime = previousState != GameState.WAITING
                || session.isCountdownRunning()
                || session.isEndingScheduled()
                || session.isWinResolved();

        session.reset();

        if (hadActiveRuntime) {
            logEmptyReset(arena);
        }

        refreshDisplay(arena);
        refreshScoreboards(arena);
    }

    public void handleQuit(Player player) {
        if (player == null || joinManager == null) {
            return;
        }
        joinManager.leave(player, false);
    }

    public void handleKill(Player killer, Player victim) {
        if (killer == null || victim == null || joinManager == null) {
            return;
        }

        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        Optional<Arena> killerArena = getArena(killer);
        Optional<Arena> victimArena = getArena(victim);
        if (killerArena.isEmpty() || victimArena.isEmpty()) {
            return;
        }

        Arena arena = killerArena.get();
        if (!arena.id().equals(victimArena.get().id())) {
            return;
        }

        if (getArenaState(arena) != GameState.INGAME) {
            return;
        }

        advanceProgress(killer, arena);
    }

    public void advanceProgress(Player killer, Arena arena) {
        if (killer == null || arena == null) {
            return;
        }

        GameSession session = getSession(arena).orElse(null);
        if (session == null || session.getState() != GameState.INGAME || session.isWinResolved()) {
            return;
        }

        PlayerProgress progress = session.getOrCreateProgress(killer.getUniqueId());
        progress.incrementTotalKills();
        progress.addKillOnCurrentLevel();

        int killsRequired = levelManager.getKillsRequired(progress.getLevel());
        messages.sendPrefixed(killer, "kill.progress", progressPlaceholders(killer, arena, progress));

        refreshScoreboards(arena);

        if (progress.getKillsOnCurrentLevel() < killsRequired) {
            return;
        }

        int maxLevel = levelManager.getMaxLevel();
        if (progress.getLevel() < maxLevel) {
            levelUp(killer, arena, progress);
            if (isBroadcastLevelUpEnabled()) {
                Map<String, String> broadcastPlaceholders = progressPlaceholders(killer, arena, progress);
                broadcastPlaceholders.put("player", killer.getName());
                broadcast(arena, "level.up-broadcast", broadcastPlaceholders);
            }
            return;
        }

        winGame(killer, arena);
    }

    public void levelUp(Player player, Arena arena, PlayerProgress progress) {
        if (player == null || arena == null || progress == null) {
            return;
        }

        progress.setLevel(progress.getLevel() + 1);
        progress.resetKillsOnCurrentLevel();
        equipPlayer(player, arena, progress.getLevel());
        messages.sendPrefixed(player, "level.up", progressPlaceholders(player, arena, progress));
        refreshScoreboards(arena);
    }

    public void winGame(Player winner, Arena arena) {
        if (winner == null || arena == null) {
            return;
        }

        GameSession session = getSession(arena).orElse(null);
        if (session == null || session.getState() != GameState.INGAME || session.isWinResolved()) {
            return;
        }

        session.setWinResolved(true);
        session.setState(GameState.ENDING);
        refreshDisplay(arena);

        Map<String, String> winnerPlaceholders = progressPlaceholders(winner, arena,
                session.getOrCreateProgress(winner.getUniqueId()));
        winnerPlaceholders.put("player", winner.getName());
        broadcast(arena, "game.winner", winnerPlaceholders);

        int endingSeconds = endingSeconds();
        Map<String, String> endingPlaceholders = placeholders(arena, endingSeconds);
        endingPlaceholders.put("player", winner.getName());
        broadcast(arena, "game.ending", endingPlaceholders);

        session.cancelEnding();
        BukkitTask endingTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> finishMatch(arena),
                endingSeconds * 20L
        );
        session.setEndingTask(endingTask);
        refreshScoreboards(arena);
    }

    public List<RankedProgressEntry> getRankedProgress(Arena arena) {
        if (arena == null) {
            return List.of();
        }

        GameSession session = sessions.get(arena.id());
        if (session == null) {
            return List.of();
        }

        List<RankedProgressEntry> entries = new ArrayList<>();
        for (UUID playerId : session.getPlayers()) {
            PlayerProgress progress = session.getProgress(playerId)
                    .orElseGet(() -> defaultLobbyProgress(playerId));
            Player player = Bukkit.getPlayer(playerId);
            entries.add(new RankedProgressEntry(playerId, player, progress));
        }

        entries.sort(RankedProgressEntry.comparator());
        return List.copyOf(entries);
    }

    public void handleRespawn(Player player) {
        if (player == null || !isMatchActive(player)) {
            return;
        }

        getArena(player).ifPresent(arena ->
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    PlayerMatchState.prepareAfterRespawn(player);
                    equipPlayer(player, arena);
                }, 1L)
        );
    }

    public boolean shouldClearInventoryOnLeave() {
        return configManager.config().getBoolean(CLEAR_INVENTORY_ON_LEAVE_PATH, true);
    }

    public void clearPlayerInventory(Player player) {
        if (player != null) {
            player.getInventory().clear();
        }
    }

    public Optional<PlayerProgress> getProgress(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return getProgress(player.getUniqueId());
    }

    public Optional<PlayerProgress> getProgress(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }

        for (GameSession session : sessions.values()) {
            Optional<PlayerProgress> progress = session.getProgress(playerId);
            if (progress.isPresent()) {
                return progress;
            }
        }
        return Optional.empty();
    }

    public Optional<PlayerProgress> getProgress(Arena arena, UUID playerId) {
        if (arena == null || playerId == null) {
            return Optional.empty();
        }
        return getSession(arena).flatMap(session -> session.getProgress(playerId));
    }

    public void resetProgress(Arena arena) {
        if (arena == null) {
            return;
        }

        GameSession session = getOrCreateSession(arena);
        session.resetProgress();
        for (UUID playerId : session.getPlayers()) {
            session.getOrCreateProgress(playerId).reset();
        }
    }

    public void equipPlayer(Player player, Arena arena) {
        if (player == null || arena == null) {
            return;
        }

        PlayerProgress progress = getOrCreateSession(arena).getOrCreateProgress(player.getUniqueId());
        equipPlayer(player, arena, progress.getLevel());
    }

    public void equipArenaPlayers(Arena arena) {
        for (Player player : getOnlinePlayers(arena)) {
            equipPlayer(player, arena);
        }
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
        session.cancelEnding();
        session.setWinResolved(false);
        finishMatch(arena);
        broadcast(arena, "game.stopped", placeholders(arena));
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
        refreshScoreboards(arena);
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
        refreshScoreboards(arena);
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
        resetProgress(arena);

        for (Player player : onlinePlayers) {
            teleportToRandomSpawn(player, arena);
            PlayerMatchState.prepareForMatch(player);
            equipPlayer(player, arena, 1);
        }

        broadcast(arena, "game.started", placeholders(arena));
        refreshDisplay(arena);
        refreshScoreboards(arena);
    }

    private void equipPlayer(Player player, Arena arena, int level) {
        Optional<LevelDefinition> levelDefinition = levelManager.getLevel(level);
        if (levelDefinition.isEmpty()) {
            messages.sendPrefixed(player, "level.missing-config", Map.of("level", String.valueOf(level)));
            return;
        }

        List<ItemStack> itemStacks = levelManager.createItemStacks(level);
        if (itemStacks.isEmpty()) {
            messages.sendPrefixed(player, "level.missing-config", Map.of("level", String.valueOf(level)));
            return;
        }

        clearInventory(player);

        PlayerInventory inventory = player.getInventory();
        inventory.setItem(0, itemStacks.getFirst().clone());

        for (int index = 1; index < itemStacks.size(); index++) {
            inventory.addItem(itemStacks.get(index).clone());
        }

        inventory.setHeldItemSlot(0);

        LevelDefinition definition = levelDefinition.get();
        messages.sendPrefixed(player, "level.equipped", Map.of(
                "level", String.valueOf(definition.level()),
                "kills_required", String.valueOf(definition.killsRequired()),
                "weapon", stripColor(definition.weapon().displayName())
        ));
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

    public boolean isMatchActive(Player player) {
        if (player == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        for (GameSession session : sessions.values()) {
            GameState state = session.getState();
            if ((state == GameState.INGAME || state == GameState.ENDING) && session.hasPlayer(playerId)) {
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

        if (isSessionEmpty(arena)) {
            resetEmptySession(arena);
            return;
        }

        if (getOnlinePlayers(arena).size() < arena.minPlayers()) {
            cancelCountdown(arena);
            session.setState(GameState.WAITING);
            broadcast(arena, "game.countdown-cancelled", placeholders(arena));
            refreshDisplay(arena);
            refreshScoreboards(arena);
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

    private void clearInventory(Player player) {
        if (player != null) {
            player.getInventory().clear();
        }
    }

    private void clearInventories(Arena arena) {
        for (Player player : getOnlinePlayers(arena)) {
            clearInventory(player);
        }
    }

    private boolean isClearInventoryOnStopEnabled() {
        return configManager.config().getBoolean(CLEAR_INVENTORY_ON_STOP_PATH, true);
    }

    private boolean isBroadcastLevelUpEnabled() {
        return configManager.config().getBoolean(BROADCAST_LEVEL_UP_PATH, true);
    }

    private int endingSeconds() {
        return Math.max(1, configManager.config().getInt(ENDING_SECONDS_PATH, 5));
    }

    private void finishMatch(Arena arena) {
        if (arena == null) {
            return;
        }

        if (spectatorManager != null) {
            spectatorManager.restoreArena(arena);
        }

        GameSession session = getOrCreateSession(arena);
        session.cancelCountdown();
        session.cancelEnding();

        if (isClearInventoryOnStopEnabled()) {
            clearInventories(arena);
        }

        teleportPlayersToLobby(arena);
        resetProgress(arena);

        session.setWinResolved(false);
        session.setState(GameState.WAITING);
        refreshDisplay(arena);
        refreshScoreboards(arena);
    }

    private static PlayerProgress defaultLobbyProgress(UUID playerId) {
        PlayerProgress progress = new PlayerProgress(playerId);
        progress.reset();
        return progress;
    }

    private void refreshScoreboards(Arena arena) {
        if (scoreboardManager != null && arena != null) {
            scoreboardManager.updateArena(arena);
        }
    }

    private Map<String, String> progressPlaceholders(Player player, Arena arena, PlayerProgress progress) {
        Map<String, String> values = new HashMap<>(placeholders(arena));
        values.put("player", player.getName());
        values.put("level", String.valueOf(progress.getLevel()));
        values.put("max_level", String.valueOf(levelManager.getMaxLevel()));
        values.put("kills", String.valueOf(progress.getKillsOnCurrentLevel()));
        values.put("kills_required", String.valueOf(levelManager.getKillsRequired(progress.getLevel())));
        values.put("total_kills", String.valueOf(progress.getTotalKills()));
        values.put("weapon", weaponNameForLevel(progress.getLevel()));
        return values;
    }

    private String weaponNameForLevel(int level) {
        return levelManager.getLevel(level)
                .map(definition -> stripColor(definition.weapon().displayName()))
                .orElse("Weapon");
    }

    private static String stripColor(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
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

    private void logEmptyReset(Arena arena) {
        String formatted = messages.format("game.empty-reset", Map.of(
                "arena", arena.displayName(),
                "id", arena.id()
        ));
        if (formatted.isEmpty()) {
            plugin.getLogger().info("[Water Fight] Arena '" + arena.id() + "' reset because it became empty.");
        } else {
            plugin.getLogger().info(formatted);
        }
    }
}
