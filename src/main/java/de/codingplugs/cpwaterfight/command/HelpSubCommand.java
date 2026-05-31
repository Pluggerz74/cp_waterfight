package de.codingplugs.cpwaterfight.command;

import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class HelpSubCommand implements SubCommand {

    private final MessageManager messages;

    public HelpSubCommand(MessageManager messages) {
        this.messages = messages;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String permission() {
        return CommandPermissions.USE;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(messages.component("help.header"));

        if (sender.hasPermission(CommandPermissions.JOIN)) {
            messages.sendLines(sender, "help.player", Map.of());
        }

        if (sender.hasPermission(CommandPermissions.ADMIN)) {
            messages.sendLines(sender, "help.admin", Map.of());
        } else if (sender.hasPermission(CommandPermissions.USE)) {
            sender.sendMessage(messages.component("help.reload-line"));
        }

        sender.sendMessage(messages.component("help.footer"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
