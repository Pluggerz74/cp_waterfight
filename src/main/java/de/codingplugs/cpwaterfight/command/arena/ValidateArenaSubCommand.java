package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.arena.ArenaValidationEntry;
import de.codingplugs.cpwaterfight.arena.ArenaValidationResult;
import de.codingplugs.cpwaterfight.command.AdminSubCommand;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class ValidateArenaSubCommand extends AdminSubCommand {

    private final ArenaManager arenaManager;

    public ValidateArenaSubCommand(MessageManager messages, ArenaManager arenaManager) {
        super(messages);
        this.arenaManager = arenaManager;
    }

    @Override
    public String name() {
        return "validate";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(sender, args, 1, messages, "arena.usage.validate")) {
            return true;
        }

        String id = args[1];
        Arena arena = arenaManager.getArena(id).orElse(null);
        if (arena == null) {
            messages.sendPrefixed(sender, "arena.not-found", Map.of("id", id));
            return true;
        }

        ArenaValidationResult result = arenaManager.validateArena(arena);
        messages.sendPrefixed(sender, "arena.validation-header", Map.of(
                "id", arena.id(),
                "display", arena.displayName()
        ));

        for (ArenaValidationEntry entry : result.entries()) {
            String path = entry.valid()
                    ? "arena.validation-line-valid"
                    : "arena.validation-line-invalid";
            messages.send(sender, path, Map.of("check", entry.label()));
        }

        String footerPath = result.ready()
                ? "arena.validation-ready"
                : "arena.validation-not-ready";
        messages.sendPrefixed(sender, footerPath, Map.of(
                "id", arena.id(),
                "display", arena.displayName()
        ));
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
