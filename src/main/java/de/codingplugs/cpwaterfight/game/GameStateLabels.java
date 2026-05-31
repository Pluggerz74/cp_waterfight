package de.codingplugs.cpwaterfight.game;

import de.codingplugs.cpwaterfight.config.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.Map;

/**
 * Resolves user-facing labels for {@link GameState} values from configuration.
 */
public final class GameStateLabels {

    private static final String CONFIG_PATH = "state-labels";

    private final ConfigManager configManager;
    private Map<GameState, String> labels = defaultLabels();

    public GameStateLabels(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void load() {
        reload();
    }

    public void reload() {
        Map<GameState, String> loaded = new EnumMap<>(GameState.class);
        ConfigurationSection section = configManager.config().getConfigurationSection(CONFIG_PATH);

        for (GameState state : GameState.values()) {
            String configured = section != null ? section.getString(state.name()) : null;
            loaded.put(state, configured != null && !configured.isBlank() ? configured : defaultLabel(state));
        }

        labels = loaded;
    }

    public String label(GameState state) {
        if (state == null) {
            return defaultLabel(GameState.WAITING);
        }
        return labels.getOrDefault(state, defaultLabel(state));
    }

    private static Map<GameState, String> defaultLabels() {
        Map<GameState, String> defaults = new EnumMap<>(GameState.class);
        for (GameState state : GameState.values()) {
            defaults.put(state, defaultLabel(state));
        }
        return defaults;
    }

    private static String defaultLabel(GameState state) {
        return switch (state) {
            case WAITING -> "Wartet";
            case COUNTDOWN -> "Startet";
            case INGAME -> "Im Spiel";
            case ENDING -> "Beendet";
        };
    }
}
