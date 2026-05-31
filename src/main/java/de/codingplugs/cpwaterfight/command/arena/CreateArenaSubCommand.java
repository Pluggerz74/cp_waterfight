package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.AdminSubCommand;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CreateArenaSubCommand extends AdminSubCommand {

    private final ArenaManager arenaManager;

    public CreateArenaSubCommand(MessageManager messages, ArenaManager arenaManager) {
        super(messages);
        this.arenaManager = arenaManager;
    }

    @Override
    public String name() {
        return "create";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(sender, args, 1, messages, "arena.usage.create")) {
            return true;
        }

        String id = args[1];
        if (!arenaManager.isValidId(id)) {
            messages.sendPrefixed(sender, "arena.invalid-id");
            return true;
        }

        if (arenaManager.exists(id)) {
            messages.sendPrefixed(sender, "arena.already-exists", Map.of("id", id.toLowerCase()));
            return true;
        }

        arenaManager.createArena(id).ifPresentOrElse(
                arena -> messages.sendPrefixed(sender, "arena.created", Map.of("id", arena.id())),
                () -> messages.sendPrefixed(sender, "arena.create-failed")
        );
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
