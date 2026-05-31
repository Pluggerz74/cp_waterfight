package de.codingplugs.cpwaterfight.command;

import de.codingplugs.cpwaterfight.message.MessageManager;

public abstract class AdminSubCommand implements SubCommand {

    protected final MessageManager messages;

    protected AdminSubCommand(MessageManager messages) {
        this.messages = messages;
    }

    @Override
    public String permission() {
        return CommandPermissions.ADMIN;
    }

    protected void sendUsage(org.bukkit.command.CommandSender sender, String messagePath) {
        messages.sendPrefixed(sender, messagePath);
    }
}
