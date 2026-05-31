package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.ArenaManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ArenaCommandSupport {

    private ArenaCommandSupport() {
    }

    public static List<String> tabCompleteArenaIds(ArenaManager arenaManager, String input) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return arenaManager.getArenaIds().stream()
                .filter(id -> id.startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }

    public static boolean requireArenaArgument(
            CommandSender sender,
            String[] args,
            int index,
            de.codingplugs.cpwaterfight.message.MessageManager messages,
            String usagePath
    ) {
        if (args.length <= index || args[index].isBlank()) {
            messages.sendPrefixed(sender, usagePath);
            return false;
        }
        return true;
    }
}
