package de.codingplugs.cpwaterfight.spectator;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.game.GameState;
import de.codingplugs.cpwaterfight.game.PlayerMatchState;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Brief post-death spectator phase before respawn and re-equip during active matches.
 */
public final class SpectatorManager {

    private final JavaPlugin plugin;
    private final SpectatorSettings settings;
    private final GameManager gameManager;
    private final JoinManager joinManager;
    private final MessageManager messages;

    private final Map<UUID, SpectatorState> states = new HashMap<>();

    public SpectatorManager(
            JavaPlugin plugin,
            SpectatorSettings settings,
            GameManager gameManager,
            JoinManager joinManager,
            MessageManager messages
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
        this.joinManager = Objects.requireNonNull(joinManager, "joinManager");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public boolean isEnabled() {
        return settings.enabled();
    }

    public void handleDeath(Player victim, Player killer, Arena arena) {
        if (!settings.enabled() || victim == null || arena == null) {
            return;
        }

        if (gameManager.getArenaState(arena) != GameState.INGAME) {
            return;
        }

        UUID playerId = victim.getUniqueId();
        cancelState(playerId);

        SpectatorState state = new SpectatorState(
                arena.id(),
                victim.getGameMode(),
                victim.getAllowFlight(),
                victim.isFlying()
        );
        states.put(playerId, state);

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> enterSpectatorMode(victim, killer, arena, state),
                1L
        );

        state.restoreTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> restore(victim),
                1L + settings.durationTicks()
        );
    }

    public void restore(Player player) {
        restore(player, true);
    }

    public void restore(Player player, boolean reEquip) {
        if (player == null) {
            return;
        }

        SpectatorState state = states.remove(player.getUniqueId());
        if (state == null) {
            return;
        }

        cancelTask(state);

        if (!player.isOnline()) {
            return;
        }

        restoreVisibility(player, state);
        restoreMovementState(player, state);

        boolean shouldReEquip = reEquip
                && gameManager.isInGame(player)
                && joinManager.isInArena(player)
                && arenaId(player).map(state.arenaId()::equals).orElse(false);

        if (shouldReEquip) {
            gameManager.getArena(player).ifPresent(arena -> {
                PlayerMatchState.prepareForMatch(player);
                gameManager.teleportToRandomSpawn(player, arena);
                gameManager.equipPlayer(player, arena);
            });
            sendEndMessage(player, state);
        } else {
            applySafeGameMode(player, state.originalGameMode());
        }
    }

    public void restoreAll() {
        for (UUID playerId : new ArrayList<>(states.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                restore(player, false);
            } else {
                cancelState(playerId);
            }
        }
    }

    public void restoreArena(Arena arena) {
        restoreArena(arena, false);
    }

    public void restoreArena(Arena arena, boolean reEquip) {
        if (arena == null) {
            return;
        }

        for (UUID playerId : new ArrayList<>(states.keySet())) {
            SpectatorState state = states.get(playerId);
            if (state == null || !arena.id().equals(state.arenaId())) {
                continue;
            }

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                restore(player, reEquip);
            } else {
                cancelState(playerId);
            }
        }
    }

    public boolean isSpectating(Player player) {
        return player != null && states.containsKey(player.getUniqueId());
    }

    public int getSpectatingCount(Arena arena) {
        if (arena == null) {
            return 0;
        }

        int count = 0;
        for (SpectatorState state : states.values()) {
            if (arena.id().equals(state.arenaId())) {
                count++;
            }
        }
        return count;
    }

    public void shutdown() {
        restoreAll();
    }

    private void enterSpectatorMode(Player victim, Player killer, Arena arena, SpectatorState state) {
        if (!victim.isOnline() || !states.containsKey(victim.getUniqueId())) {
            return;
        }

        if (gameManager.getArenaState(arena) != GameState.INGAME) {
            restore(victim, false);
            return;
        }

        victim.getInventory().clear();

        if (settings.teleportToKiller() && isValidKiller(killer, victim, arena)) {
            victim.teleport(killer.getLocation());
        }

        if (settings.hideFromPlayers()) {
            hideFromArenaPlayers(victim, arena, state);
        }

        victim.setGameMode(settings.gamemode());

        sendStartFeedback(victim, killer, arena);
    }

    private void hideFromArenaPlayers(Player victim, Arena arena, SpectatorState state) {
        for (Player other : gameManager.getOnlinePlayers(arena)) {
            if (other.equals(victim)) {
                continue;
            }

            other.hidePlayer(plugin, victim);
            state.hiddenFrom().add(other.getUniqueId());
        }
    }

    private void restoreVisibility(Player player, SpectatorState state) {
        for (UUID otherId : state.hiddenFrom()) {
            Player other = Bukkit.getPlayer(otherId);
            if (other != null && other.isOnline()) {
                other.showPlayer(plugin, player);
            }
        }
    }

    private void restoreMovementState(Player player, SpectatorState state) {
        applySafeGameMode(player, state.originalGameMode());
        player.setAllowFlight(state.originalAllowFlight());
        player.setFlying(state.originalFlying() && state.originalAllowFlight());
    }

    private void applySafeGameMode(Player player, GameMode original) {
        GameMode restored = original == GameMode.CREATIVE || original == GameMode.SPECTATOR
                ? GameMode.SURVIVAL
                : original;
        player.setGameMode(restored);
    }

    private void sendStartFeedback(Player victim, Player killer, Arena arena) {
        Map<String, String> placeholders = spectatorPlaceholders(killer, arena);

        if (!messages.format("spectator.start", placeholders).isEmpty()) {
            messages.sendPrefixed(victim, "spectator.start", placeholders);
        }

        if (!settings.sendTitle()) {
            return;
        }

        Component title = messages.component("spectator.title", placeholders);
        Component subtitle = messages.component("spectator.subtitle", placeholders);
        if (title.equals(Component.empty()) && subtitle.equals(Component.empty())) {
            return;
        }

        victim.showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(250),
                        Duration.ofMillis(settings.durationTicks() * 50L),
                        Duration.ofMillis(250)
                )
        ));
    }

    private void sendEndMessage(Player player, SpectatorState state) {
        gameManager.getArena(player).ifPresent(arena -> {
            Map<String, String> placeholders = spectatorPlaceholders(null, arena);
            if (!messages.format("spectator.end", placeholders).isEmpty()) {
                messages.sendPrefixed(player, "spectator.end", placeholders);
            }
        });
    }

    private Map<String, String> spectatorPlaceholders(Player killer, Arena arena) {
        Map<String, String> values = new HashMap<>();
        values.put("seconds", String.valueOf(settings.durationSeconds()));
        values.put("killer", killer != null ? killer.getName() : "Unknown");
        values.put("arena", arena != null ? arena.displayName() : "");
        return values;
    }

    private boolean isValidKiller(Player killer, Player victim, Arena arena) {
        if (killer == null || !killer.isOnline() || killer.equals(victim)) {
            return false;
        }

        return joinManager.getArena(killer)
                .map(killerArena -> killerArena.id().equals(arena.id()))
                .orElse(false);
    }

    private java.util.Optional<String> arenaId(Player player) {
        return joinManager.getArena(player).map(Arena::id);
    }

    private void cancelState(UUID playerId) {
        SpectatorState state = states.remove(playerId);
        if (state != null) {
            cancelTask(state);
        }
    }

    private static void cancelTask(SpectatorState state) {
        if (state.restoreTask != null) {
            state.restoreTask.cancel();
            state.restoreTask = null;
        }
    }

    private static final class SpectatorState {

        private final String arenaId;
        private final GameMode originalGameMode;
        private final boolean originalAllowFlight;
        private final boolean originalFlying;
        private final Set<UUID> hiddenFrom = new HashSet<>();
        private BukkitTask restoreTask;

        private SpectatorState(
                String arenaId,
                GameMode originalGameMode,
                boolean originalAllowFlight,
                boolean originalFlying
        ) {
            this.arenaId = arenaId;
            this.originalGameMode = originalGameMode;
            this.originalAllowFlight = originalAllowFlight;
            this.originalFlying = originalFlying;
        }

        private String arenaId() {
            return arenaId;
        }

        private GameMode originalGameMode() {
            return originalGameMode;
        }

        private boolean originalAllowFlight() {
            return originalAllowFlight;
        }

        private boolean originalFlying() {
            return originalFlying;
        }

        private Set<UUID> hiddenFrom() {
            return hiddenFrom;
        }
    }
}
