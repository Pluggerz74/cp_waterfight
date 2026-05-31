package de.codingplugs.cpwaterfight.game;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * Resets basic player state when a Water Fight match starts.
 */
public final class PlayerMatchState {

    private PlayerMatchState() {
    }

    public static void prepareForMatch(Player player) {
        if (player == null) {
            return;
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);

        double maxHealth = player.getMaxHealth();
        player.setHealth(maxHealth);
    }
}
