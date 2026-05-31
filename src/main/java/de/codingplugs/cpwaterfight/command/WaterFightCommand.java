package de.codingplugs.cpwaterfight.command;

import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class WaterFightCommand implements CommandExecutor, TabCompleter {

    private final MessageManager messages;
    private final Map<String, SubCommand> subCommands;
    private final HelpSubCommand defaultSubCommand;

    public WaterFightCommand(MessageManager messages, List<SubCommand> commands) {
        this.messages = messages;
        this.subCommands = new LinkedHashMap<>();
        HelpSubCommand help = null;

        for (SubCommand subCommand : commands) {
            subCommands.put(subCommand.name().toLowerCase(Locale.ROOT), subCommand);
            if (subCommand instanceof HelpSubCommand helpSubCommand) {
                help = helpSubCommand;
            }
        }

        if (help == null) {
            throw new IllegalArgumentException("HelpSubCommand is required.");
        }
        this.defaultSubCommand = help;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return executeSubCommand(sender, defaultSubCommand, args);
        }

        String input = args[0].toLowerCase(Locale.ROOT);
        SubCommand subCommand = subCommands.get(input);
        if (subCommand == null) {
            messages.sendPrefixed(sender, "general.unknown-command");
            return true;
        }

        return executeSubCommand(sender, subCommand, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return subCommands.values().stream()
                    .filter(sub -> sub.name().startsWith(prefix))
                    .filter(sub -> sender.hasPermission(sub.permission()))
                    .map(SubCommand::name)
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length >= 2) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
            if (subCommand != null && sender.hasPermission(subCommand.permission())) {
                return subCommand.tabComplete(sender, args);
            }
        }

        return Collections.emptyList();
    }

    private boolean executeSubCommand(CommandSender sender, SubCommand subCommand, String[] args) {
        if (!sender.hasPermission(subCommand.permission())) {
            messages.sendPrefixed(sender, "general.no-permission");
            return true;
        }

        return subCommand.execute(sender, args);
    }
}
