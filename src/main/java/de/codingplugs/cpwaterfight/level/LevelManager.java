package de.codingplugs.cpwaterfight.level;

import de.codingplugs.cpwaterfight.config.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Loads and provides Water Fight level and weapon definitions.
 */
public final class LevelManager {

    private static final int DEFAULT_MAX_LEVEL = 20;
    private static final int DEFAULT_KILLS_REQUIRED = 2;

    private final ConfigManager configManager;
    private final Logger logger;
    private final LevelConfigParser parser;

    private final Map<Integer, LevelDefinition> levels = new LinkedHashMap<>();

    private int maxLevel = DEFAULT_MAX_LEVEL;
    private int defaultKillsRequired = DEFAULT_KILLS_REQUIRED;

    public LevelManager(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
        this.parser = new LevelConfigParser(logger);
    }

    public void load() {
        reload();
    }

    public void reload() {
        levels.clear();
        FileConfiguration config = configManager.levels();
        ConfigurationSection root = config;

        maxLevel = parser.parseMaxLevel(root, DEFAULT_MAX_LEVEL);
        defaultKillsRequired = parser.parseDefaultKillsRequired(root, DEFAULT_KILLS_REQUIRED);

        ConfigurationSection levelsSection = config.getConfigurationSection("levels");
        if (levelsSection == null) {
            logger.warning("levels.yml has no 'levels' section. GunGame progression is unavailable.");
            validateLevels();
            return;
        }

        for (String key : levelsSection.getKeys(false)) {
            int levelNumber = parseLevelKey(key);
            if (levelNumber < 1 || levelNumber > maxLevel) {
                logger.warning("Skipping level key '" + key + "' (expected 1.." + maxLevel + ").");
                continue;
            }

            ConfigurationSection levelSection = levelsSection.getConfigurationSection(key);
            if (levelSection == null) {
                logger.warning("Level " + levelNumber + " section is empty.");
                continue;
            }

            LevelDefinition definition = parser.parseLevel(levelNumber, levelSection, defaultKillsRequired);
            levels.put(levelNumber, definition);
        }

        validateLevels();
    }

    public void shutdown() {
        levels.clear();
    }

    public Optional<LevelDefinition> getLevel(int level) {
        if (level < 1) {
            return Optional.empty();
        }
        return Optional.ofNullable(levels.get(level));
    }

    public List<LevelDefinition> getLevels() {
        List<LevelDefinition> sorted = new ArrayList<>(levels.values());
        sorted.sort((first, second) -> Integer.compare(first.level(), second.level()));
        return Collections.unmodifiableList(sorted);
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getKillsRequired(int level) {
        return getLevel(level).map(LevelDefinition::killsRequired).orElse(defaultKillsRequired);
    }

    public LevelDefinition getDefaultLevel() {
        return getLevel(1).orElseGet(this::createFallbackLevel);
    }

    public int getDefaultKillsRequired() {
        return defaultKillsRequired;
    }

    /**
     * Creates item stacks for a level kit. Does not give items to players.
     */
    public List<ItemStack> createItemStacks(int level) {
        return getLevel(level)
                .map(LevelDefinition::createItemStacks)
                .orElseGet(() -> getDefaultLevel().createItemStacks());
    }

    public ItemStack createItemStack(int level) {
        return getLevel(level)
                .map(LevelDefinition::createItemStack)
                .orElseGet(() -> getDefaultLevel().createItemStack());
    }

    public void validateLevels() {
        for (int level = 1; level <= maxLevel; level++) {
            if (!levels.containsKey(level)) {
                logger.warning("levels.yml is missing level " + level + ".");
            }
        }

        if (levels.isEmpty()) {
            logger.warning("No valid levels were loaded from levels.yml.");
        } else {
            logger.info("Loaded " + levels.size() + " Water Fight levels (max level " + maxLevel + ").");
        }
    }

    public ConfigManager configManager() {
        return configManager;
    }

    private int parseLevelKey(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private LevelDefinition createFallbackLevel() {
        return new LevelDefinition(
                1,
                defaultKillsRequired,
                new WeaponDefinition(
                        org.bukkit.Material.WOODEN_SWORD,
                        "&7Holzschwert",
                        1,
                        List.of("&7Level %level%", "&7Kills bis nächste Waffe: &f%kills_required%"),
                        true,
                        Map.of(),
                        Map.of()
                )
        );
    }
}
