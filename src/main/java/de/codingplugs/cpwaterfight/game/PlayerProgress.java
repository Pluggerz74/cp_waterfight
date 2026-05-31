package de.codingplugs.cpwaterfight.game;

import java.util.Objects;
import java.util.UUID;

/**
 * Per-player GunGame progression during an active Water Fight session.
 */
public final class PlayerProgress {

    private final UUID playerId;
    private int level = 1;
    private int killsOnCurrentLevel;
    private int totalKills;

    public PlayerProgress(UUID playerId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
    }

    public UUID playerId() {
        return playerId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getKillsOnCurrentLevel() {
        return killsOnCurrentLevel;
    }

    public void addKillOnCurrentLevel() {
        killsOnCurrentLevel++;
    }

    public void resetKillsOnCurrentLevel() {
        killsOnCurrentLevel = 0;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public void incrementTotalKills() {
        totalKills++;
    }

    public void reset() {
        level = 1;
        killsOnCurrentLevel = 0;
        totalKills = 0;
    }
}
