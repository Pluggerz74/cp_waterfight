package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.display.JoinDisplayManager;
import de.codingplugs.cpwaterfight.join.JoinManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static Optional<Integer> parsePositiveInt(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        try {
            int value = Integer.parseInt(input.trim());
            if (value < 1) {
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static String joinArgs(String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return "";
        }

        return String.join(" ", Stream.of(args).skip(startIndex).toList()).trim();
    }

    public static void refreshJoinDisplay(Arena arena, JoinManager joinManager, JoinDisplayManager joinDisplayManager) {
        if (arena == null || joinManager == null || joinDisplayManager == null) {
            return;
        }

        joinDisplayManager.refreshArena(arena, joinManager.getPlayerCount(arena));
    }
}
