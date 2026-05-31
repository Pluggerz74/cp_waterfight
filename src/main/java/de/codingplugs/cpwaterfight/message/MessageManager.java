package de.codingplugs.cpwaterfight.message;

import de.codingplugs.cpwaterfight.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.Map;

/**
 * Resolves and sends configurable messages using legacy {@code &} color codes.
 */
public final class MessageManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final ConfigManager configManager;

    private String prefix = "";

    public MessageManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void load() {
        reload();
    }

    public void reload() {
        FileConfiguration messages = configManager.messages();
        prefix = messages.getString("prefix", "&b&lWater Fight &7» &r");
    }

    public String raw(String path) {
        return configManager.messages().getString(path, "");
    }

    public String format(String path) {
        return format(path, Collections.emptyMap());
    }

    public String format(String path, Map<String, String> placeholders) {
        String message = raw(path);
        if (message.isEmpty()) {
            return "";
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        return message;
    }

    public Component component(String path) {
        return component(path, Collections.emptyMap());
    }

    public Component component(String path, Map<String, String> placeholders) {
        return LEGACY.deserialize(format(path, placeholders));
    }

    public Component componentFromRaw(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return Component.empty();
        }
        return LEGACY.deserialize(rawText);
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Collections.emptyMap());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(component(path, placeholders));
    }

    public void sendPrefixed(CommandSender sender, String path) {
        sendPrefixed(sender, path, Collections.emptyMap());
    }

    public void sendPrefixed(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(LEGACY.deserialize(prefix + format(path, placeholders)));
    }

    public void sendLines(CommandSender sender, String path, Map<String, String> placeholders) {
        String content = format(path, placeholders);
        if (content.isEmpty()) {
            return;
        }

        for (String line : content.split("\n")) {
            sender.sendMessage(LEGACY.deserialize(line));
        }
    }
}
