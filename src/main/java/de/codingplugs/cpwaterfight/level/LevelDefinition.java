package de.codingplugs.cpwaterfight.level;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * A single Water Fight progression level.
 */
public final class LevelDefinition {

    private final int level;
    private final int killsRequired;
    private final WeaponDefinition weapon;

    public LevelDefinition(int level, int killsRequired, WeaponDefinition weapon) {
        this.level = Math.max(1, level);
        this.killsRequired = Math.max(1, killsRequired);
        this.weapon = Objects.requireNonNull(weapon, "weapon");
    }

    public int level() {
        return level;
    }

    public int killsRequired() {
        return killsRequired;
    }

    public WeaponDefinition weapon() {
        return weapon;
    }

    /**
     * Creates item stacks for this level's weapon kit.
     */
    public List<ItemStack> createItemStacks() {
        return weapon.createItemStacks(this);
    }

    public ItemStack createItemStack() {
        return weapon.createItemStack(this);
    }
}
