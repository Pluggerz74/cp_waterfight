package de.codingplugs.cpwaterfight.listener;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handles quit cleanup, in-game respawns, kills, and pre-start fall damage protection.
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
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (gameManager.shouldHideDeathMessages()) {
            Arena victimArena = gameManager.getArena(victim).orElse(null);
            if (victimArena != null) {
                GameState state = gameManager.getArenaState(victimArena);
                if (state == GameState.INGAME || state == GameState.ENDING) {
                    event.deathMessage(null);
                }
            }
        }

        if (killer == null) {
            return;
        }

        gameManager.handleKill(killer, victim);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!gameManager.isMatchActive(player)) {
            return;
        }

        gameManager.getArena(player).ifPresent(arena ->
                gameManager.getRandomSpawn(arena).ifPresent(event::setRespawnLocation)
        );

        gameManager.handleRespawn(player);
    }
}
