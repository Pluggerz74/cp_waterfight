package de.codingplugs.cpwaterfight.level;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses {@code levels.yml} into level and weapon definitions.
 */
final class LevelConfigParser {

    private final Logger logger;

    LevelConfigParser(Logger logger) {
        this.logger = logger;
    }

    int parseMaxLevel(ConfigurationSection root, int defaultValue) {
        ConfigurationSection settings = root.getConfigurationSection("settings");
        int maxLevel = settings != null ? settings.getInt("max-level", defaultValue) : defaultValue;
        return Math.max(1, maxLevel);
    }

    int parseDefaultKillsRequired(ConfigurationSection root, int defaultValue) {
        ConfigurationSection settings = root.getConfigurationSection("settings");
        int kills = settings != null ? settings.getInt("default-kills-required", defaultValue) : defaultValue;
        return Math.max(1, kills);
    }

    LevelDefinition parseLevel(int levelNumber, ConfigurationSection section, int defaultKillsRequired) {
        int killsRequired = Math.max(1, section.getInt("kills-required", defaultKillsRequired));
        ConfigurationSection weaponSection = section.getConfigurationSection("weapon");
        if (weaponSection == null) {
            logger.log(Level.WARNING, "Level " + levelNumber + " is missing a weapon section.");
            return new LevelDefinition(levelNumber, killsRequired, fallbackWeapon());
        }

        WeaponDefinition weapon = parseWeapon(levelNumber, weaponSection);
        return new LevelDefinition(levelNumber, killsRequired, weapon);
    }

    private WeaponDefinition parseWeapon(int levelNumber, ConfigurationSection section) {
        Material material = parseMaterial(section.getString("material"), levelNumber);
        String name = section.getString("name", "&7Weapon");
        int amount = section.getInt("amount", 1);
        boolean unbreakable = section.getBoolean("unbreakable", true);
        List<String> lore = section.getStringList("lore");

        Map<Enchantment, Integer> enchantments = parseEnchantments(
                section.getConfigurationSection("enchantments"),
                levelNumber
        );
        Map<Material, Integer> extraItems = parseExtraItems(
                section.getConfigurationSection("extra-items"),
                levelNumber
        );

        return new WeaponDefinition(material, name, amount, lore, unbreakable, enchantments, extraItems);
    }

    private Material parseMaterial(String raw, int levelNumber) {
        if (raw == null || raw.isBlank()) {
            logger.warning("Level " + levelNumber + " has no weapon material. Using WOODEN_SWORD.");
            return Material.WOODEN_SWORD;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            logger.warning("Level " + levelNumber + " has invalid material '" + raw + "'. Using WOODEN_SWORD.");
            return Material.WOODEN_SWORD;
        }
    }

    private Map<Enchantment, Integer> parseEnchantments(ConfigurationSection section, int levelNumber) {
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        if (section == null) {
            return enchantments;
        }

        for (String key : section.getKeys(false)) {
            Enchantment enchantment = resolveEnchantment(key);
            if (enchantment == null) {
                logger.warning("Level " + levelNumber + " has invalid enchantment '" + key + "'. Skipping.");
                continue;
            }

            int level = Math.max(1, section.getInt(key, 1));
            enchantments.put(enchantment, level);
        }

        return enchantments;
    }

    private Map<Material, Integer> parseExtraItems(ConfigurationSection section, int levelNumber) {
        Map<Material, Integer> extraItems = new LinkedHashMap<>();
        if (section == null) {
            return extraItems;
        }

        for (String key : section.getKeys(false)) {
            Material material = parseMaterial(key, levelNumber);
            int amount = Math.max(1, Math.min(64, section.getInt(key, 1)));
            extraItems.put(material, amount);
        }

        return extraItems;
    }

    private Enchantment resolveEnchantment(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.minecraft(normalized);
        Enchantment enchantment = Registry.ENCHANTMENT.get(key);
        if (enchantment != null) {
            return enchantment;
        }

        @SuppressWarnings("deprecation")
        Enchantment legacy = Enchantment.getByName(raw.trim().toUpperCase(Locale.ROOT));
        return legacy;
    }

    private WeaponDefinition fallbackWeapon() {
        return new WeaponDefinition(
                Material.WOODEN_SWORD,
                "&7Fallback Weapon",
                1,
                List.of("&7Level %level%"),
                true,
                Map.of(),
                Map.of()
        );
    }
}
