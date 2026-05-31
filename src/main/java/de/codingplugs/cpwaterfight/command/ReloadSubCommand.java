package de.codingplugs.cpwaterfight.command;

import de.codingplugs.cpwaterfight.CPWaterFight;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class ReloadSubCommand implements SubCommand {

    private static final String PERMISSION = "cpwaterfight.admin";

    private final CPWaterFight plugin;
    private final MessageManager messages;

    public ReloadSubCommand(CPWaterFight plugin, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return PERMISSION;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        try {
            plugin.reload();
            messages.sendPrefixed(sender, "general.reload-success");
        } catch (Exception exception) {
            plugin.getLogger().severe("Reload failed: " + exception.getMessage());
            messages.sendPrefixed(sender, "general.reload-failure");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
