package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.AdminSubCommand;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ArenaListSubCommand extends AdminSubCommand {

    private final ArenaManager arenaManager;

    public ArenaListSubCommand(MessageManager messages, ArenaManager arenaManager) {
        super(messages);
        this.arenaManager = arenaManager;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        List<String> ids = arenaManager.getArenaIds();
        if (ids.isEmpty()) {
            messages.sendPrefixed(sender, "arena.list-empty");
            return true;
        }

        messages.sendPrefixed(sender, "arena.list", Map.of(
                "count", String.valueOf(ids.size()),
                "ids", ids.stream().collect(Collectors.joining("&7, &f"))
        ));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
