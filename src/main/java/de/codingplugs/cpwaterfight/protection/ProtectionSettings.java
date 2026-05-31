package de.codingplugs.cpwaterfight.protection;

import de.codingplugs.cpwaterfight.config.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Config-driven Water Fight protection toggles.
 */
public final class ProtectionSettings {

    private static final String ROOT = "protection.";

    private final ConfigManager configManager;

    public ProtectionSettings(ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
    }

    public boolean blockItemDrop() {
        return config().getBoolean(ROOT + "block-item-drop", true);
    }

    public boolean blockItemPickup() {
        return config().getBoolean(ROOT + "block-item-pickup", true);
    }

    public boolean blockInventoryClick() {
        return config().getBoolean(ROOT + "block-inventory-click", true);
    }

    public boolean blockOffhandSwap() {
        return config().getBoolean(ROOT + "block-offhand-swap", true);
    }

    public boolean blockBreak() {
        return config().getBoolean(ROOT + "block-break", true);
    }

    public boolean blockPlace() {
        return config().getBoolean(ROOT + "block-place", true);
    }

    public boolean lockFoodLevel() {
        return config().getBoolean(ROOT + "lock-food-level", true);
    }

    public boolean preventFireDamageBeforeStart() {
        return config().getBoolean(ROOT + "prevent-fire-damage-before-start", true);
    }

    public boolean preventDrowningBeforeStart() {
        return config().getBoolean(ROOT + "prevent-drowning-before-start", true);
    }

    public boolean preventItemDurabilityLoss() {
        return config().getBoolean(ROOT + "prevent-item-durability-loss", true);
    }

    public boolean restrictCommandsIngame() {
        return config().getBoolean(ROOT + "restrict-commands-ingame", true);
    }

    public long messageCooldownMillis() {
        return Math.max(0L, config().getLong(ROOT + "message-cooldown-millis", 1500L));
    }

    public String summary() {
        List<String> enabled = new ArrayList<>();
        if (blockItemDrop()) {
            enabled.add("drop");
        }
        if (blockItemPickup()) {
            enabled.add("pickup");
        }
        if (blockInventoryClick()) {
            enabled.add("inventory");
        }
        if (blockOffhandSwap()) {
            enabled.add("offhand");
        }
        if (blockBreak()) {
            enabled.add("break");
        }
        if (blockPlace()) {
            enabled.add("place");
        }
        if (lockFoodLevel()) {
            enabled.add("food");
        }
        if (preventItemDurabilityLoss()) {
            enabled.add("durability");
        }
        if (restrictCommandsIngame()) {
            enabled.add("commands");
        }
        if (preventFireDamageBeforeStart()) {
            enabled.add("pre-fire");
        }
        if (preventDrowningBeforeStart()) {
            enabled.add("pre-drown");
        }

        if (enabled.isEmpty()) {
            return "none active";
        }
        return String.join(", ", enabled);
    }

    public Set<String> allowedCommandsIngame() {
        List<String> configured = config().getStringList(ROOT + "allowed-commands-ingame");
        if (configured.isEmpty()) {
            return Set.of("wf", "waterfight", "msg", "tell", "r");
        }

        return configured.stream()
                .filter(command -> command != null && !command.isBlank())
                .map(command -> command.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private FileConfiguration config() {
        return configManager.config();
    }
}
