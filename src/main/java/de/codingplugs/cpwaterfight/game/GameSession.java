package de.codingplugs.cpwaterfight.game;

import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime session state for one Water Fight arena.
 */
public final class GameSession {

    private final String arenaId;
    private final Set<UUID> players = new LinkedHashSet<>();
    private final Map<UUID, PlayerProgress> progressByPlayer = new HashMap<>();
    private GameState state = GameState.WAITING;

    private BukkitTask countdownTask;
    private int countdownSecondsRemaining;

    public GameSession(String arenaId) {
        this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
    }

    public String arenaId() {
        return arenaId;
    }

    public void addPlayer(UUID uuid) {
        if (uuid != null) {
            players.add(uuid);
        }
    }

    public void removePlayer(UUID uuid) {
        if (uuid != null) {
            players.remove(uuid);
            progressByPlayer.remove(uuid);
        }
    }

    public boolean hasPlayer(UUID uuid) {
        return uuid != null && players.contains(uuid);
    }

    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public Optional<PlayerProgress> getProgress(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(progressByPlayer.get(playerId));
    }

    public PlayerProgress getOrCreateProgress(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return progressByPlayer.computeIfAbsent(playerId, PlayerProgress::new);
    }

    public void resetProgress() {
        progressByPlayer.clear();
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state != null ? state : GameState.WAITING;
    }

    public int getCountdownSecondsRemaining() {
        return countdownSecondsRemaining;
    }

    public void setCountdownSecondsRemaining(int countdownSecondsRemaining) {
        this.countdownSecondsRemaining = Math.max(0, countdownSecondsRemaining);
    }

    public boolean isCountdownRunning() {
        return countdownTask != null && !countdownTask.isCancelled();
    }

    public void setCountdownTask(BukkitTask countdownTask) {
        cancelCountdown();
        this.countdownTask = countdownTask;
    }

    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    public void resetCountdown() {
        cancelCountdown();
        countdownSecondsRemaining = 0;
    }

    public void reset() {
        resetCountdown();
        players.clear();
        resetProgress();
        state = GameState.WAITING;
    }
}
