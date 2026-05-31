package de.codingplugs.cpwaterfight.config;

import de.codingplugs.cpwaterfight.CPWaterFight;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

/**
 * Loads, caches, and persists plugin YAML configuration files.
 */
public final class ConfigManager {

    public static final String CONFIG_FILE = "config.yml";
    public static final String ARENAS_FILE = "arenas.yml";
    public static final String LEVELS_FILE = "levels.yml";
    public static final String MESSAGES_FILE = "messages.yml";

    private static final List<String> ALL_FILES = List.of(
            CONFIG_FILE,
            ARENAS_FILE,
            LEVELS_FILE,
            MESSAGES_FILE
    );

    private final CPWaterFight plugin;

    private FileConfiguration config;
    private FileConfiguration arenas;
    private FileConfiguration levels;
    private FileConfiguration messages;

    public ConfigManager(CPWaterFight plugin) {
        this.plugin = plugin;
    }

    public boolean load() {
        try {
            ensureDataFolder();
            installDefaultConfigs();
            reload();
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration", exception);
            return false;
        }
    }

    public void reload() {
        config = loadConfig(CONFIG_FILE);
        arenas = loadConfig(ARENAS_FILE);
        levels = loadConfig(LEVELS_FILE);
        messages = loadConfig(MESSAGES_FILE);
    }

    public void shutdown() {
        // Reserved for future persistence hooks.
    }

    public void save(String fileName, FileConfiguration configuration) {
        File file = new File(plugin.getDataFolder(), fileName);
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + fileName, exception);
        }
    }

    public FileConfiguration config() {
        return config;
    }

    public FileConfiguration arenas() {
        return arenas;
    }

    public FileConfiguration levels() {
        return levels;
    }

    public FileConfiguration messages() {
        return messages;
    }

    private void ensureDataFolder() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }
    }

    private void installDefaultConfigs() {
        for (String fileName : ALL_FILES) {
            File target = new File(plugin.getDataFolder(), fileName);
            if (!target.exists()) {
                plugin.saveResource(fileName, false);
            }
        }
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        applyDefaults(yaml, fileName);
        return yaml;
    }

    private void applyDefaults(YamlConfiguration target, String fileName) {
        InputStream stream = plugin.getResource(fileName);
        if (stream == null) {
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            target.setDefaults(defaults);
            target.options().copyDefaults(true);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not apply defaults for " + fileName, exception);
        }
    }
}
