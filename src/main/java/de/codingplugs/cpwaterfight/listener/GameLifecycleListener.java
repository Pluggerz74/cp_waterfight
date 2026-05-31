package de.codingplugs.cpwaterfight.listener;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handles quit cleanup, in-game respawns, and pre-start fall damage protection.
 */
public final class GameLifecycleListener implements Listener {

    private final GameManager gameManager;

    public GameLifecycleListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!gameManager.isInGame(player)) {
            return;
        }

        gameManager.getArena(player).ifPresent(arena ->
                gameManager.getRandomSpawn(arena).ifPresent(event::setRespawnLocation)
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        if (!gameManager.isFallDamageProtectionEnabled()) {
            return;
        }

        Arena arena = gameManager.getArena(player).orElse(null);
        if (arena == null) {
            return;
        }

        GameState state = gameManager.getArenaState(arena);
        if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }
}
