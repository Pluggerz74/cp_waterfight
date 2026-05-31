package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.AdminSubCommand;
import de.codingplugs.cpwaterfight.display.JoinDisplayManager;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class RenameArenaSubCommand extends AdminSubCommand {

    private final ArenaManager arenaManager;
    private final JoinManager joinManager;
    private final JoinDisplayManager joinDisplayManager;

    public RenameArenaSubCommand(
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
        return "rename";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(sender, args, 1, messages, "arena.usage.rename")) {
            return true;
        }

        if (args.length < 3) {
            sendUsage(sender, "arena.usage.rename");
            return true;
        }

        String id = args[1];
        if (!arenaManager.exists(id)) {
            messages.sendPrefixed(sender, "arena.not-found", Map.of("id", id));
            return true;
        }

        String displayName = ArenaCommandSupport.joinArgs(args, 2);
        if (displayName.isBlank()) {
            sendUsage(sender, "arena.usage.rename");
            return true;
        }

        if (!arenaManager.renameArena(id, displayName)) {
            messages.sendPrefixed(sender, "arena.not-found", Map.of("id", id));
            return true;
        }

        Arena arena = arenaManager.getArena(id).orElse(null);
        if (arena != null) {
            ArenaCommandSupport.refreshJoinDisplay(arena, joinManager, joinDisplayManager);
            messages.sendPrefixed(sender, "arena.renamed", Map.of(
                    "id", arena.id(),
                    "display", arena.displayName()
            ));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return ArenaCommandSupport.tabCompleteArenaIds(arenaManager, args[1]);
        }
        return List.of();
    }
}
