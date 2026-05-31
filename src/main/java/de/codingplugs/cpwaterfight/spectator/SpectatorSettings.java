package de.codingplugs.cpwaterfight.spectator;

import de.codingplugs.cpwaterfight.config.ConfigManager;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.Objects;

/**
 * Config-driven spectator behaviour after in-game deaths.
 */
public final class SpectatorSettings {

    private static final String ROOT = "spectator.";

    private final ConfigManager configManager;

    public SpectatorSettings(ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
    }

    public boolean enabled() {
        return config().getBoolean(ROOT + "enabled", true);
    }

    public int durationTicks() {
        return Math.max(1, config().getInt(ROOT + "duration-ticks", 40));
    }

    public GameMode gamemode() {
        String value = config().getString(ROOT + "gamemode", "SPECTATOR");
        if (value == null) {
            return GameMode.SPECTATOR;
        }

        return switch (value.toUpperCase(Locale.ROOT)) {
            case "ADVENTURE" -> GameMode.ADVENTURE;
            case "SPECTATOR" -> GameMode.SPECTATOR;
            default -> GameMode.SPECTATOR;
        };
    }

    public boolean hideFromPlayers() {
        return config().getBoolean(ROOT + "hide-from-players", false);
    }

    public boolean teleportToKiller() {
        return config().getBoolean(ROOT + "teleport-to-killer", false);
    }

    public boolean sendTitle() {
        return config().getBoolean(ROOT + "send-title", true);
    }

    public int durationSeconds() {
        return Math.max(1, (durationTicks() + 19) / 20);
    }

    private FileConfiguration config() {
        return configManager.config();
    }
}
