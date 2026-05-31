package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.PlayerSubCommand;
import de.codingplugs.cpwaterfight.display.JoinDisplayManager;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SetJoinSubCommand extends PlayerSubCommand {

    private static final int TARGET_RANGE = 6;

    private final ArenaManager arenaManager;
    private final JoinManager joinManager;
    private final JoinDisplayManager joinDisplayManager;

    public SetJoinSubCommand(
            MessageManager messages,
            ArenaManager arenaManager,
            JoinManager joinManager,
            JoinDisplayManager joinDisplayManager
    ) {
        super(messages);
        this.arenaManager = arenaManager;
        this.joinManager = joinManager;
        this.joinDisplayManager = joinDisplayManager;
    }

    @Override
    public String name() {
        return "setjoin";
    }

    @Override
    protected boolean executePlayer(Player player, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(player, args, 1, messages, "arena.usage.setjoin")) {
            return true;
        }

        String id = args[1];
        Arena arena = arenaManager.getArena(id).orElse(null);
        if (arena == null) {
            messages.sendPrefixed(player, "arena.not-found", Map.of("id", id));
            return true;
        }

        Block target = player.getTargetBlockExact(TARGET_RANGE);
        if (target == null || target.getType().isAir()) {
            messages.sendPrefixed(player, "arena.no-target-block");
            return true;
        }

        if (arenaManager.setJoinBlock(id, target.getLocation())) {
            String normalizedId = id.toLowerCase(Locale.ROOT);
            arenaManager.getArena(normalizedId).ifPresent(refreshed -> joinDisplayManager.refreshArena(
                    refreshed,
                    joinManager.getPlayerCount(refreshed)
            ));
            messages.sendPrefixed(player, "arena.join-set", Map.of("id", normalizedId));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length == 2) {
            return ArenaCommandSupport.tabCompleteArenaIds(arenaManager, args[1]);
        }
        return List.of();
    }
}
