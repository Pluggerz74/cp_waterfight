package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.AdminSubCommand;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class ArenaInfoSubCommand extends AdminSubCommand {

    private final ArenaManager arenaManager;

    public ArenaInfoSubCommand(MessageManager messages, ArenaManager arenaManager) {
        super(messages);
        this.arenaManager = arenaManager;
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(sender, args, 1, messages, "arena.usage.info")) {
            return true;
        }

        String id = args[1];
        Arena arena = arenaManager.getArena(id).orElse(null);
        if (arena == null) {
            messages.sendPrefixed(sender, "arena.not-found", Map.of("id", id));
            return true;
        }

        messages.sendLines(sender, "arena.info", Map.of(
                "id", arena.id(),
                "display", arena.displayName(),
                "state", arena.state().name(),
                "min", String.valueOf(arena.minPlayers()),
                "max", String.valueOf(arena.maxPlayers()),
                "lobby", arena.hasLobby() ? "&ayes" : "&cno",
                "join", arena.hasJoinBlock() ? "&ayes" : "&cno",
                "spawns", String.valueOf(arena.spawnCount())
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
