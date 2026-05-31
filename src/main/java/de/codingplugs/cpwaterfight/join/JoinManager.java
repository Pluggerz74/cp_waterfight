package de.codingplugs.cpwaterfight.join;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.display.JoinDisplayManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks players in Water Fight arenas and handles join/leave flow.
 */
public final class JoinManager {

    private final MessageManager messages;
    private final ArenaManager arenaManager;
    private final JoinDisplayManager joinDisplayManager;

    private final Map<UUID, String> playerArenas = new HashMap<>();

    public JoinManager(
            MessageManager messages,
            ArenaManager arenaManager,
            JoinDisplayManager joinDisplayManager
    ) {
        this.messages = messages;
        this.arenaManager = arenaManager;
        this.joinDisplayManager = joinDisplayManager;
    }

    public void load() {
        clear();
    }

    public void reload() {
        clear();
    }

    public void shutdown() {
        clear();
    }

    public void clear() {
        playerArenas.clear();
    }

    public void removeArenaPlayers(String arenaId) {
        if (arenaId == null) {
            return;
        }
        playerArenas.entrySet().removeIf(entry -> arenaId.equals(entry.getValue()));
    }

    public boolean join(Player player, Arena arena) {
        if (player == null || arena == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        String arenaId = arena.id();

        Optional<String> currentArenaId = getArenaId(player);
        if (currentArenaId.isPresent()) {
            if (currentArenaId.get().equals(arenaId)) {
                messages.sendPrefixed(player, "join.already-in-arena", Map.of("arena", arena.displayName()));
                return false;
            }
            leave(player, false);
        }

        if (!arena.hasLobby()) {
            messages.sendPrefixed(player, "join.lobby-missing", Map.of("arena", arena.displayName()));
            return false;
        }

        if (getPlayerCount(arena) >= arena.maxPlayers()) {
            messages.sendPrefixed(player, "join.arena-full", Map.of(
                    "arena", arena.displayName(),
                    "max", String.valueOf(arena.maxPlayers())
            ));
            return false;
        }

        playerArenas.put(playerId, arenaId);
        teleportToLobby(player, arena);
        messages.sendPrefixed(player, "join.joined-arena", Map.of("arena", arena.displayName()));
        joinDisplayManager.updateArena(arena, getPlayerCount(arena));
        return true;
    }

    public boolean leave(Player player) {
        return leave(player, true);
    }

    public boolean leave(Player player, boolean notify) {
        if (player == null) {
            return false;
        }

        String arenaId = playerArenas.remove(player.getUniqueId());
        if (arenaId == null) {
            if (notify) {
                messages.sendPrefixed(player, "join.not-in-arena");
            }
            return false;
        }

        arenaManager.getArena(arenaId).ifPresent(arena -> {
            if (notify) {
                messages.sendPrefixed(player, "join.left-arena", Map.of("arena", arena.displayName()));
            }
            joinDisplayManager.updateArena(arena, getPlayerCount(arena));
        });
        return true;
    }

    public Optional<Arena> getArena(Player player) {
        return getArenaId(player).flatMap(arenaManager::getArena);
    }

    public Optional<String> getArenaId(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playerArenas.get(player.getUniqueId()));
    }

    public int getPlayerCount(Arena arena) {
        if (arena == null) {
            return 0;
        }

        String arenaId = arena.id();
        int count = 0;
        for (String value : playerArenas.values()) {
            if (arenaId.equals(value)) {
                count++;
            }
        }
        return count;
    }

    public boolean isInArena(Player player) {
        return player != null && playerArenas.containsKey(player.getUniqueId());
    }

    private void teleportToLobby(Player player, Arena arena) {
        Location lobby = arena.lobby();
        if (lobby == null || lobby.getWorld() == null) {
            return;
        }
        player.teleport(lobby);
    }
}
