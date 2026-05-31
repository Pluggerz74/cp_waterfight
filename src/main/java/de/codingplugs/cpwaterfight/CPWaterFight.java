package de.codingplugs.cpwaterfight;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

/**
 * Entry point for the Water Fight minigame plugin.
 */
public final class CPWaterFight extends JavaPlugin {

    public static final String GAME_MODE_NAME = "Water Fight";

    private static final String CONFIG_FILE = "config.yml";
    private static final String ARENAS_FILE = "arenas.yml";
    private static final String LEVELS_FILE = "levels.yml";
    private static final String MESSAGES_FILE = "messages.yml";

    private static final List<String> CONFIG_FILES = List.of(
            CONFIG_FILE,
            ARENAS_FILE,
            LEVELS_FILE,
            MESSAGES_FILE
    );

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private FileConfiguration config;
    private FileConfiguration arenas;
    private FileConfiguration levels;
    private FileConfiguration messages;

    private boolean enabled;

    @Override
    public void onEnable() {
        if (!bootstrap()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        enabled = true;
        getLogger().info(GAME_MODE_NAME + " foundation loaded.");
    }

    @Override
    public void onDisable() {
        if (!enabled) {
            return;
        }

        enabled = false;
        getLogger().info(GAME_MODE_NAME + " shut down.");
    }

    /**
     * Loads configuration files and prepares the plugin runtime.
     *
     * @return {@code true} when startup succeeded
     */
    private boolean bootstrap() {
        try {
            ensureDataFolder();
            installDefaultConfigs();
            reloadAllConfigs();
            return true;
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to start " + GAME_MODE_NAME, exception);
            return false;
        }
    }

    private void ensureDataFolder() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }
    }

    private void installDefaultConfigs() {
        for (String fileName : CONFIG_FILES) {
            File target = new File(getDataFolder(), fileName);
            if (!target.exists()) {
                saveResource(fileName, false);
            }
        }
    }

    /**
     * Reloads every plugin configuration from disk.
     */
    public void reloadAllConfigs() {
        config = loadConfig(CONFIG_FILE);
        arenas = loadConfig(ARENAS_FILE);
        levels = loadConfig(LEVELS_FILE);
        messages = loadConfig(MESSAGES_FILE);
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(getDataFolder(), fileName);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        applyDefaults(yaml, fileName);
        return yaml;
    }

    private void applyDefaults(YamlConfiguration target, String fileName) {
        InputStream stream = getResource(fileName);
        if (stream == null) {
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            target.setDefaults(defaults);
            target.options().copyDefaults(true);
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Could not apply defaults for " + fileName, exception);
        }
    }

    /**
     * Persists a configuration file to the plugin data folder.
     *
     * @param fileName configuration file name
     * @param configuration configuration to save
     */
    public void saveConfig(String fileName, FileConfiguration configuration) {
        File file = new File(getDataFolder(), fileName);
        try {
            configuration.save(file);
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Could not save " + fileName, exception);
        }
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public FileConfiguration getArenasConfig() {
        return arenas;
    }

    public FileConfiguration getLevelsConfig() {
        return levels;
    }

    public FileConfiguration getMessagesConfig() {
        return messages;
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }

    public boolean isPluginReady() {
        return enabled;
    }
}
