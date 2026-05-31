package de.codingplugs.cpwaterfight.feedback;

import de.codingplugs.cpwaterfight.message.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;

/**
 * Configurable title/subtitle with timing and placeholder support.
 */
public final class TitleDefinition {

    private final boolean enabled;
    private final String titlePath;
    private final String subtitlePath;
    private final int fadeInTicks;
    private final int stayTicks;
    private final int fadeOutTicks;

    private TitleDefinition(
            boolean enabled,
            String titlePath,
            String subtitlePath,
            int fadeInTicks,
            int stayTicks,
            int fadeOutTicks
    ) {
        this.enabled = enabled;
        this.titlePath = titlePath;
        this.subtitlePath = subtitlePath;
        this.fadeInTicks = fadeInTicks;
        this.stayTicks = stayTicks;
        this.fadeOutTicks = fadeOutTicks;
    }

    public static TitleDefinition fromConfig(ConfigurationSection section, TitleDefinition defaults) {
        if (section == null) {
            return defaults != null ? defaults : disabled();
        }

        if (!section.getBoolean("enabled", true)) {
            return disabled();
        }

        String title = section.getString("title", defaults != null ? defaults.titlePath : "");
        String subtitle = section.getString("subtitle", defaults != null ? defaults.subtitlePath : "");
        int fadeIn = section.getInt("fade-in", defaults != null ? defaults.fadeInTicks : 5);
        int stay = section.getInt("stay", defaults != null ? defaults.stayTicks : 30);
        int fadeOut = section.getInt("fade-out", defaults != null ? defaults.fadeOutTicks : 10);

        return new TitleDefinition(true, title, subtitle, fadeIn, stay, fadeOut);
    }

    public static TitleDefinition disabled() {
        return new TitleDefinition(false, "", "", 0, 0, 0);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void show(Player player, MessageManager messages, Map<String, String> placeholders) {
        if (!enabled || player == null || !player.isOnline() || messages == null) {
            return;
        }

        Component title = messages.componentFromRaw(applyPlaceholders(titlePath, placeholders));
        Component subtitle = messages.componentFromRaw(applyPlaceholders(subtitlePath, placeholders));
        if (title.equals(Component.empty()) && subtitle.equals(Component.empty())) {
            return;
        }

        player.showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(fadeInTicks * 50L),
                        Duration.ofMillis(stayTicks * 50L),
                        Duration.ofMillis(fadeOutTicks * 50L)
                )
        ));
    }

    private static String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }
}
