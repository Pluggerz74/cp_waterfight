package de.codingplugs.cpwaterfight.game;

import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.UUID;

/**
 * Player progress entry for ranking and future scoreboard display.
 */
public record RankedProgressEntry(UUID playerId, Player player, PlayerProgress progress) {

    public static Comparator<RankedProgressEntry> comparator() {
        return Comparator
                .comparingInt((RankedProgressEntry entry) -> entry.progress().getLevel()).reversed()
                .thenComparingInt(entry -> entry.progress().getKillsOnCurrentLevel()).reversed()
                .thenComparingInt(entry -> entry.progress().getTotalKills()).reversed();
    }
}
