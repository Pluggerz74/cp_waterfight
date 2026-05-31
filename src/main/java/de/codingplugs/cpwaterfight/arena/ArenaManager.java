package de.codingplugs.cpwaterfight.arena;

import de.codingplugs.cpwaterfight.config.ConfigManager;

/**
 * Manages Water Fight arenas. Arena setup will be implemented in a later step.
 */
public final class ArenaManager {

    private final ConfigManager configManager;

    public ArenaManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void load() {
        // Arena definitions are read from arenas.yml when setup is implemented.
    }

    public void reload() {
        load();
    }

    public void shutdown() {
        // Reserved for future arena persistence.
    }

    public ConfigManager configManager() {
        return configManager;
    }
}
