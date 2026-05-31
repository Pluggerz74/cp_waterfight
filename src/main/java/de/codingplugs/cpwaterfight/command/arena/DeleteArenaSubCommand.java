package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.AdminSubCommand;
import de.codingplugs.cpwaterfight.display.JoinDisplayManager;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DeleteArenaSubCommand extends AdminSubCommand {

    private final ArenaManager arenaManager;
    private final JoinManager joinManager;
    private final JoinDisplayManager joinDisplayManager;

    public DeleteArenaSubCommand(
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
        return "delete";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(sender, args, 1, messages, "arena.usage.delete")) {
            return true;
        }

        String id = args[1];
        if (!arenaManager.exists(id)) {
            messages.sendPrefixed(sender, "arena.not-found", Map.of("id", id));
            return true;
        }

        String normalizedId = id.toLowerCase(Locale.ROOT);
        if (arenaManager.deleteArena(id)) {
            joinManager.removeArenaPlayers(normalizedId);
            joinDisplayManager.removeArena(normalizedId);
            messages.sendPrefixed(sender, "arena.deleted", Map.of("id", normalizedId));
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
