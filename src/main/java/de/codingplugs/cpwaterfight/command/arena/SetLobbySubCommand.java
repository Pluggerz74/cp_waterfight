package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.PlayerSubCommand;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class SetLobbySubCommand extends PlayerSubCommand {

    private final ArenaManager arenaManager;

    public SetLobbySubCommand(MessageManager messages, ArenaManager arenaManager) {
        super(messages);
        this.arenaManager = arenaManager;
    }

    @Override
    public String name() {
        return "setlobby";
    }

    @Override
    protected boolean executePlayer(Player player, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(player, args, 1, messages, "arena.usage.setlobby")) {
            return true;
        }

        String id = args[1];
        if (!arenaManager.exists(id)) {
            messages.sendPrefixed(player, "arena.not-found", Map.of("id", id));
            return true;
        }

        if (arenaManager.setLobby(id, player.getLocation())) {
            messages.sendPrefixed(player, "arena.lobby-set", Map.of("id", id.toLowerCase()));
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
