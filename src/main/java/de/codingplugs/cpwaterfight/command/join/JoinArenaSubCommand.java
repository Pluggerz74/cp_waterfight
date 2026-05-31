package de.codingplugs.cpwaterfight.command.join;

import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.arena.ArenaCommandSupport;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class JoinArenaSubCommand extends JoinPlayerSubCommand {

    private final ArenaManager arenaManager;
    private final JoinManager joinManager;

    public JoinArenaSubCommand(MessageManager messages, ArenaManager arenaManager, JoinManager joinManager) {
        super(messages);
        this.arenaManager = arenaManager;
        this.joinManager = joinManager;
    }

    @Override
    public String name() {
        return "join";
    }

    @Override
    protected boolean executePlayer(Player player, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(player, args, 1, messages, "join.usage-join")) {
            return true;
        }

        String id = args[1];
        arenaManager.getArena(id).ifPresentOrElse(
                arena -> joinManager.join(player, arena),
                () -> messages.sendPrefixed(player, "arena.not-found", Map.of("id", id))
        );
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
