package de.codingplugs.cpwaterfight.level;

import de.codingplugs.cpwaterfight.config.ConfigManager;

/**
 * Manages level progression and weapons. Full level logic comes in a later step.
 */
public final class LevelManager {

    private final ConfigManager configManager;

    public LevelManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void load() {
        // Level definitions are read from levels.yml when progression is implemented.
    }

    public void reload() {
        load();
    }

    public void shutdown() {
        // Reserved for future level cache cleanup.
    }

    public ConfigManager configManager() {
        return configManager;
    }
}
