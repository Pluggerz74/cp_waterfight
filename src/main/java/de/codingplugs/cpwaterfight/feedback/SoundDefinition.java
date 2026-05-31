package de.codingplugs.cpwaterfight.feedback;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Configurable sound effect with safe parsing and playback.
 */
public final class SoundDefinition {

    private final boolean enabled;
    private final Sound sound;
    private final float volume;
    private final float pitch;

    private SoundDefinition(boolean enabled, Sound sound, float volume, float pitch) {
        this.enabled = enabled;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public static SoundDefinition fromConfig(
            ConfigurationSection section,
            String defaultSound,
            float defaultVolume,
            float defaultPitch,
            Logger logger,
            String label
    ) {
        if (section == null) {
            return disabled();
        }

        boolean enabled = section.getBoolean("enabled", true);
        if (!enabled) {
            return disabled();
        }

        String soundName = section.getString("sound", defaultSound);
        Sound sound = parseSound(soundName, logger, label);
        if (sound == null) {
            return disabled();
        }

        float volume = (float) section.getDouble("volume", defaultVolume);
        float pitch = (float) section.getDouble("pitch", defaultPitch);
        return new SoundDefinition(true, sound, volume, pitch);
    }

    public static SoundDefinition disabled() {
        return new SoundDefinition(false, null, 0.0F, 0.0F);
    }

    public boolean isEnabled() {
        return enabled && sound != null;
    }

    public void play(Player player) {
        if (!isEnabled() || player == null || !player.isOnline()) {
            return;
        }

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static Sound parseSound(String soundName, Logger logger, String label) {
        if (soundName == null || soundName.isBlank()) {
            if (logger != null) {
                logger.warning("[Water Fight] Feedback sound '" + label + "' has no sound name configured.");
            }
            return null;
        }

        try {
            return Sound.valueOf(soundName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            if (logger != null) {
                logger.warning("[Water Fight] Unknown feedback sound '" + soundName + "' for '" + label + "'.");
            }
            return null;
        }
    }
}
