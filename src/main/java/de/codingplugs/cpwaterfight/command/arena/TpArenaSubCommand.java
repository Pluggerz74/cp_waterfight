package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.PlayerSubCommand;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class TpArenaSubCommand extends PlayerSubCommand {

    private final ArenaManager arenaManager;

    public TpArenaSubCommand(MessageManager messages, ArenaManager arenaManager) {
        super(messages);
        this.arenaManager = arenaManager;
    }

    @Override
    public String name() {
        return "tp";
    }

    @Override
    protected boolean executePlayer(Player player, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(player, args, 1, messages, "arena.usage.tp")) {
            return true;
        }

        String id = args[1];
        Arena arena = arenaManager.getArena(id).orElse(null);
        if (arena == null) {
            messages.sendPrefixed(player, "arena.not-found", Map.of("id", id));
            return true;
        }

        if (!arena.hasLobby()) {
            messages.sendPrefixed(player, "arena.lobby-missing", Map.of(
                    "arena", arena.displayName(),
                    "id", arena.id()
            ));
            return true;
        }

        Location lobby = arena.lobby();
        if (lobby == null || lobby.getWorld() == null) {
            messages.sendPrefixed(player, "arena.lobby-missing", Map.of(
                    "arena", arena.displayName(),
                    "id", arena.id()
            ));
            return true;
        }

        player.teleport(lobby);
        messages.sendPrefixed(player, "arena.teleported", Map.of(
                "id", arena.id(),
                "arena", arena.displayName()
        ));
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
