package de.codingplugs.cpwaterfight.listener;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.CommandPermissions;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * Handles right-click joins on configured arena join blocks.
 */
public final class JoinBlockListener implements Listener {

    private final ArenaManager arenaManager;
    private final JoinManager joinManager;
    private final MessageManager messages;

    public JoinBlockListener(ArenaManager arenaManager, JoinManager joinManager, MessageManager messages) {
        this.arenaManager = arenaManager;
        this.joinManager = joinManager;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        Optional<Arena> arena = arenaManager.findByJoinBlock(clickedBlock.getLocation());
        if (arena.isEmpty()) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission(CommandPermissions.JOIN)) {
            messages.sendPrefixed(player, "general.no-permission");
            return;
        }

        joinManager.join(player, arena.get());
    }
}
