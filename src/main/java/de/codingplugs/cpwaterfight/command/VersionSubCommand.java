package de.codingplugs.cpwaterfight.command;

import de.codingplugs.cpwaterfight.CPWaterFight;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public final class VersionSubCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final MessageManager messages;

    public VersionSubCommand(JavaPlugin plugin, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "version";
    }

    @Override
    public String permission() {
        return CommandPermissions.USE;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        messages.sendLines(sender, "general.version", Map.of(
                "plugin", plugin.getName(),
                "version", plugin.getPluginMeta().getVersion(),
                "game_mode", CPWaterFight.GAME_MODE_NAME,
                "paper", "1.21.x",
                "java", "21"
        ));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
