package de.codingplugs.cpwaterfight.config;

import de.codingplugs.cpwaterfight.level.LevelManager;
import de.codingplugs.cpwaterfight.protection.ProtectionSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Validates configuration at startup and reload, logging warnings without disabling the plugin.
 */
public final class ConfigSanityChecker {

    private static final String COUNTDOWN_PATH = "game.countdown-seconds";
    private static final String ENDING_PATH = "game.ending-seconds";
    private static final String SCOREBOARD_INTERVAL_PATH = "scoreboard.update-interval-ticks";
    private static final String JOIN_DISPLAY_HEIGHT_PATH = "join-display.height-offset";

    private final ConfigManager configManager;
    private final LevelManager levelManager;
    private final ProtectionSettings protectionSettings;

    public ConfigSanityChecker(
            ConfigManager configManager,
            LevelManager levelManager,
            ProtectionSettings protectionSettings
    ) {
        this.configManager = configManager;
        this.levelManager = levelManager;
        this.protectionSettings = protectionSettings;
    }

    public void validate(Logger logger) {
        if (logger == null) {
            return;
        }

        List<String> warnings = collectWarnings();
        if (warnings.isEmpty()) {
            logger.info("[Water Fight] Configuration sanity check passed.");
            return;
        }

        logger.warning("[Water Fight] Configuration sanity check reported " + warnings.size() + " warning(s):");
        for (String warning : warnings) {
            logger.warning("[Water Fight] " + warning);
        }
    }

    public List<String> collectWarnings() {
        List<String> warnings = new ArrayList<>();

        if (levelManager.getLevels().isEmpty()) {
            warnings.add("levels.yml defines no levels — GunGame progression will not work.");
        }

        int maxLevel = levelManager.getMaxLevel();
        if (maxLevel < 1) {
            warnings.add("levels.yml max level is below 1.");
        } else if (levelManager.getLevel(maxLevel).isEmpty()) {
            warnings.add("levels.yml is missing configuration for max level " + maxLevel + ".");
        }

        int countdownSeconds = configManager.config().getInt(COUNTDOWN_PATH, 10);
        if (countdownSeconds <= 0) {
            warnings.add("game.countdown-seconds must be positive (current: " + countdownSeconds + ").");
        }

        int endingSeconds = configManager.config().getInt(ENDING_PATH, 5);
        if (endingSeconds < 0) {
            warnings.add("game.ending-seconds must not be negative (current: " + endingSeconds + ").");
        }

        long scoreboardInterval = configManager.config().getLong(SCOREBOARD_INTERVAL_PATH, 20L);
        if (scoreboardInterval <= 0) {
            warnings.add("scoreboard.update-interval-ticks must be positive (current: " + scoreboardInterval + ").");
        }

        double joinDisplayHeight = configManager.config().getDouble(JOIN_DISPLAY_HEIGHT_PATH, 1.8D);
        if (joinDisplayHeight < 0.5D || joinDisplayHeight > 5.0D) {
            warnings.add("join-display.height-offset should be between 0.5 and 5.0 (current: " + joinDisplayHeight + ").");
        }

        if (protectionSettings.restrictCommandsIngame() && protectionSettings.allowedCommandsIngame().isEmpty()) {
            warnings.add("protection.restrict-commands-ingame is enabled but allowed-commands-ingame is empty.");
        }

        return List.copyOf(warnings);
    }
}
