package de.codingplugs.cpwaterfight.level;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configurable weapon kit for a Water Fight level.
 */
public final class WeaponDefinition {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Material FALLBACK_MATERIAL = Material.WOODEN_SWORD;

    private final Material material;
    private final String displayName;
    private final int amount;
    private final List<String> lore;
    private final boolean unbreakable;
    private final Map<Enchantment, Integer> enchantments;
    private final Map<Material, Integer> extraItems;

    public WeaponDefinition(
            Material material,
            String displayName,
            int amount,
            List<String> lore,
            boolean unbreakable,
            Map<Enchantment, Integer> enchantments,
            Map<Material, Integer> extraItems
    ) {
        this.material = material != null ? material : FALLBACK_MATERIAL;
        this.displayName = displayName != null ? displayName : "&7Weapon";
        this.amount = clampAmount(amount);
        this.lore = lore != null ? List.copyOf(lore) : List.of();
        this.unbreakable = unbreakable;
        this.enchantments = enchantments != null ? Map.copyOf(enchantments) : Map.of();
        this.extraItems = extraItems != null ? Map.copyOf(extraItems) : Map.of();
    }

    public Material material() {
        return material;
    }

    public String displayName() {
        return displayName;
    }

    public int amount() {
        return amount;
    }

    public List<String> lore() {
        return lore;
    }

    public boolean unbreakable() {
        return unbreakable;
    }

    public Map<Enchantment, Integer> enchantments() {
        return enchantments;
    }

    public Map<Material, Integer> extraItems() {
        return extraItems;
    }

    /**
     * Creates the primary weapon item for a level.
     */
    public ItemStack createItemStack(LevelDefinition level) {
        Objects.requireNonNull(level, "level");

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Map<String, String> placeholders = placeholders(level);
        meta.displayName(legacyComponent(displayName, placeholders));

        if (!lore.isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(legacyComponent(line, placeholders));
            }
            meta.lore(loreComponents);
        }

        if (unbreakable) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        if (!enchantments.isEmpty()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the primary weapon and configured extra items (for example arrows).
     */
    public List<ItemStack> createItemStacks(LevelDefinition level) {
        List<ItemStack> items = new ArrayList<>();
        items.add(createItemStack(level));

        for (Map.Entry<Material, Integer> entry : extraItems.entrySet()) {
            items.add(new ItemStack(entry.getKey(), clampAmount(entry.getValue())));
        }

        return Collections.unmodifiableList(items);
    }

    private static Map<String, String> placeholders(LevelDefinition level) {
        return Map.of(
                "level", String.valueOf(level.level()),
                "kills_required", String.valueOf(level.killsRequired()),
                "weapon", stripColor(level.weapon().displayName())
        );
    }

    private static Component legacyComponent(String input, Map<String, String> placeholders) {
        String formatted = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return LEGACY.deserialize(formatted);
    }

    private static String stripColor(String input) {
        return input.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }

    private static int clampAmount(int amount) {
        return Math.max(1, Math.min(64, amount));
    }
}
