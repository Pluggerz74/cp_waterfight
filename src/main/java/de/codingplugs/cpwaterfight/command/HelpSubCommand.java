package de.codingplugs.cpwaterfight.command;

import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class HelpSubCommand implements SubCommand {

    private static final String PERMISSION = "cpwaterfight.use";

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
        return PERMISSION;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(messages.component("help.header"));
        sender.sendMessage(messages.component("help.reload-line"));
        sender.sendMessage(messages.component("help.footer"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
