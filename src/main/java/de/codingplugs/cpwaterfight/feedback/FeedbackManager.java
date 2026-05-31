package de.codingplugs.cpwaterfight.feedback;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.game.GameSession;
import de.codingplugs.cpwaterfight.game.GameState;
import de.codingplugs.cpwaterfight.game.PlayerProgress;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.level.LevelManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Configurable sounds, titles, and actionbar feedback for Water Fight events.
 */
public final class FeedbackManager {

    private static final String ROOT = "feedback.";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messages;
    private final GameManager gameManager;
    private final JoinManager joinManager;
    private final LevelManager levelManager;
    private final Logger logger;

    private boolean enabled;
    private boolean actionBarEnabled;
    private String actionBarDuringGame;
    private String actionBarDuringCountdown;
    private long actionBarIntervalTicks;

    private SoundDefinition joinSound = SoundDefinition.disabled();
    private SoundDefinition leaveSound = SoundDefinition.disabled();
    private SoundDefinition countdownSound = SoundDefinition.disabled();
    private SoundDefinition startSound = SoundDefinition.disabled();
    private SoundDefinition killSound = SoundDefinition.disabled();
    private SoundDefinition levelUpSound = SoundDefinition.disabled();
    private SoundDefinition winSound = SoundDefinition.disabled();

    private TitleDefinition countdownTitle = TitleDefinition.disabled();
    private TitleDefinition startTitle = TitleDefinition.disabled();
    private TitleDefinition levelUpTitle = TitleDefinition.disabled();
    private TitleDefinition winTitle = TitleDefinition.disabled();

    private BukkitTask actionBarTask;

    public FeedbackManager(
            JavaPlugin plugin,
            ConfigManager configManager,
            MessageManager messages,
            GameManager gameManager,
            JoinManager joinManager,
            LevelManager levelManager
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
        this.joinManager = Objects.requireNonNull(joinManager, "joinManager");
        this.levelManager = Objects.requireNonNull(levelManager, "levelManager");
        this.logger = plugin.getLogger();
    }

    public void load() {
        reload();
    }

    public void reload() {
        shutdownActionBarTask();
        readConfig();
        if (enabled && actionBarEnabled) {
            startActionBarTask();
        }
    }

    public void shutdown() {
        shutdownActionBarTask();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void onJoin(Player player) {
        if (!enabled) {
            return;
        }
        joinSound.play(player);
    }

    public void onLeave(Player player) {
        if (!enabled) {
            return;
        }
        leaveSound.play(player);
    }

    public void onCountdownTick(Arena arena, int secondsRemaining) {
        if (!enabled || arena == null || secondsRemaining <= 0) {
            return;
        }

        Map<String, String> placeholders = arenaPlaceholders(arena, secondsRemaining);
        for (Player player : gameManager.getOnlinePlayers(arena)) {
            countdownSound.play(player);
            countdownTitle.show(player, messages, placeholders);
        }
    }

    public void onGameStart(Arena arena) {
        if (!enabled || arena == null) {
            return;
        }

        Map<String, String> placeholders = arenaPlaceholders(arena, 0);
        for (Player player : gameManager.getOnlinePlayers(arena)) {
            startSound.play(player);
            startTitle.show(player, messages, placeholders);
        }
    }

    public void onKill(Player killer, Arena arena) {
        if (!enabled || killer == null || arena == null) {
            return;
        }

        killSound.play(killer);
    }

    public void onLevelUp(Player player, Arena arena, PlayerProgress progress) {
        if (!enabled || player == null || arena == null || progress == null) {
            return;
        }

        Map<String, String> placeholders = progressPlaceholders(player, arena, progress);
        levelUpSound.play(player);
        levelUpTitle.show(player, messages, placeholders);
    }

    public void onWin(Player winner, Arena arena) {
        if (!enabled || winner == null || arena == null) {
            return;
        }

        PlayerProgress progress = gameManager.getProgress(arena, winner.getUniqueId()).orElse(null);
        Map<String, String> placeholders = progress != null
                ? progressPlaceholders(winner, arena, progress)
                : arenaPlaceholders(arena, 0);
        placeholders.put("winner", winner.getName());
        placeholders.put("player", winner.getName());

        for (Player player : gameManager.getOnlinePlayers(arena)) {
            winSound.play(player);
            winTitle.show(player, messages, placeholders);
        }
    }

    private void readConfig() {
        FileConfiguration config = configManager.config();
        enabled = config.getBoolean(ROOT + "enabled", true);

        ConfigurationSection sounds = config.getConfigurationSection(ROOT + "sounds");
        joinSound = SoundDefinition.fromConfig(
                section(sounds, "join"), "ENTITY_PLAYER_LEVELUP", 0.6F, 1.4F, logger, "join");
        leaveSound = SoundDefinition.fromConfig(
                section(sounds, "leave"), "BLOCK_NOTE_BLOCK_BASS", 0.5F, 0.8F, logger, "leave");
        countdownSound = SoundDefinition.fromConfig(
                section(sounds, "countdown"), "BLOCK_NOTE_BLOCK_PLING", 0.7F, 1.0F, logger, "countdown");
        startSound = SoundDefinition.fromConfig(
                section(sounds, "start"), "ENTITY_ENDER_DRAGON_GROWL", 0.6F, 1.2F, logger, "start");
        killSound = SoundDefinition.fromConfig(
                section(sounds, "kill"), "ENTITY_EXPERIENCE_ORB_PICKUP", 0.7F, 1.5F, logger, "kill");
        levelUpSound = SoundDefinition.fromConfig(
                section(sounds, "level-up"), "UI_TOAST_CHALLENGE_COMPLETE", 0.8F, 1.0F, logger, "level-up");
        winSound = SoundDefinition.fromConfig(
                section(sounds, "win"), "UI_TOAST_CHALLENGE_COMPLETE", 1.0F, 1.0F, logger, "win");

        ConfigurationSection titles = config.getConfigurationSection(ROOT + "titles");
        countdownTitle = TitleDefinition.fromConfig(section(titles, "countdown"), null);
        startTitle = TitleDefinition.fromConfig(section(titles, "start"), null);
        levelUpTitle = TitleDefinition.fromConfig(section(titles, "level-up"), null);
        winTitle = TitleDefinition.fromConfig(section(titles, "win"), null);

        ConfigurationSection actionBar = config.getConfigurationSection(ROOT + "actionbar");
        actionBarEnabled = enabled && actionBar != null && actionBar.getBoolean("enabled", true);
        actionBarDuringGame = actionBar != null
                ? actionBar.getString("during-game", defaultDuringGame())
                : defaultDuringGame();
        actionBarDuringCountdown = actionBar != null
                ? actionBar.getString("during-countdown", defaultDuringCountdown())
                : defaultDuringCountdown();
        actionBarIntervalTicks = Math.max(1L, actionBar != null
                ? actionBar.getLong("update-interval-ticks", 20L)
                : 20L);
    }

    private void startActionBarTask() {
        actionBarTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::updateActionBars,
                actionBarIntervalTicks,
                actionBarIntervalTicks
        );
    }

    private void shutdownActionBarTask() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    private void updateActionBars() {
        if (!enabled || !actionBarEnabled) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!joinManager.isInArena(player)) {
                continue;
            }

            joinManager.getArena(player).ifPresent(arena -> sendActionBar(player, arena));
        }
    }

    private void sendActionBar(Player player, Arena arena) {
        GameState state = gameManager.getArenaState(arena);
        String template;

        if (state == GameState.COUNTDOWN) {
            int seconds = gameManager.getSession(arena)
                    .map(GameSession::getCountdownSecondsRemaining)
                    .orElse(0);
            if (seconds <= 0) {
                return;
            }
            template = actionBarDuringCountdown;
            player.sendActionBar(messages.componentFromRaw(
                    applyPlaceholders(template, arenaPlaceholders(arena, seconds))
            ));
            return;
        }

        if (state != GameState.INGAME && state != GameState.ENDING) {
            return;
        }

        template = actionBarDuringGame;
        PlayerProgress progress = gameManager.getProgress(arena, player.getUniqueId())
                .orElseGet(() -> {
                    PlayerProgress fallback = new PlayerProgress(player.getUniqueId());
                    fallback.reset();
                    return fallback;
                });
        player.sendActionBar(messages.componentFromRaw(
                applyPlaceholders(template, progressPlaceholders(player, arena, progress))
        ));
    }

    private Map<String, String> arenaPlaceholders(Arena arena, int seconds) {
        Map<String, String> values = new HashMap<>();
        values.put("arena", arena.displayName());
        values.put("map", arena.displayName());
        values.put("seconds", String.valueOf(seconds));
        return values;
    }

    private Map<String, String> progressPlaceholders(Player player, Arena arena, PlayerProgress progress) {
        Map<String, String> values = arenaPlaceholders(arena, 0);
        values.put("player", player.getName());
        values.put("level", String.valueOf(progress.getLevel()));
        values.put("max_level", String.valueOf(levelManager.getMaxLevel()));
        values.put("kills", String.valueOf(progress.getKillsOnCurrentLevel()));
        values.put("kills_required", String.valueOf(levelManager.getKillsRequired(progress.getLevel())));
        values.put("total_kills", String.valueOf(progress.getTotalKills()));
        values.put("weapon", weaponNameForLevel(progress.getLevel()));
        return values;
    }

    private String weaponNameForLevel(int level) {
        return levelManager.getLevel(level)
                .map(definition -> stripColor(definition.weapon().displayName()))
                .orElse("Weapon");
    }

    private static String stripColor(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }

    private static String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    private static ConfigurationSection section(ConfigurationSection parent, String key) {
        return parent != null ? parent.getConfigurationSection(key) : null;
    }

    private static String defaultDuringGame() {
        return "&bLevel %level%/%max_level% &8┃ &f%weapon% &8┃ &a%kills%/%kills_required% Kills";
    }

    private static String defaultDuringCountdown() {
        return "&7Startet in &b%seconds%s";
    }
}
