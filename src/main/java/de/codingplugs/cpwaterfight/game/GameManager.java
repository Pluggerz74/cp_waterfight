package de.codingplugs.cpwaterfight.game;

import de.codingplugs.cpwaterfight.arena.Arena;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages per-arena game sessions and state transitions.
 */
public final class GameManager {

    private final Map<String, GameSession> sessions = new HashMap<>();

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
        getOrCreateSession(arena).addPlayer(player.getUniqueId());
    }

    public void handlePlayerLeave(Player player, Arena arena) {
        if (player == null || arena == null) {
            return;
        }
        getSession(arena).ifPresent(session -> session.removePlayer(player.getUniqueId()));
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

    public void removeSession(String arenaId) {
        if (arenaId == null) {
            return;
        }
        sessions.remove(arenaId);
    }
}
