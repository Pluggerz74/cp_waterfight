package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.AdminSubCommand;
import de.codingplugs.cpwaterfight.game.GameActionResult;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class ForceStartSubCommand extends AdminSubCommand {

    private final ArenaManager arenaManager;
    private final GameManager gameManager;

    public ForceStartSubCommand(MessageManager messages, ArenaManager arenaManager, GameManager gameManager) {
        super(messages);
        this.arenaManager = arenaManager;
        this.gameManager = gameManager;
    }

    @Override
    public String name() {
        return "forcestart";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(sender, args, 1, messages, "game.usage-forcestart")) {
            return true;
        }

        String id = args[1];
        arenaManager.getArena(id).ifPresentOrElse(
                arena -> sendResult(sender, gameManager.forceStart(arena), arena.displayName()),
                () -> messages.sendPrefixed(sender, "arena.not-found", Map.of("id", id))
        );
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return ArenaCommandSupport.tabCompleteArenaIds(arenaManager, args[1]);
        }
        return List.of();
    }

    private void sendResult(CommandSender sender, GameActionResult result, String arenaName) {
        String path = switch (result) {
            case SUCCESS -> "game.force-start-success";
            case ALREADY_RUNNING -> "game.already-running";
            case NO_PLAYERS -> "game.no-players";
            case MISSING_SPAWNS -> "game.missing-spawns";
            case NOT_RUNNING -> "game.not-running";
        };
        messages.sendPrefixed(sender, path, Map.of("arena", arenaName));
    }
}
