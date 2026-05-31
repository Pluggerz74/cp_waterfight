package de.codingplugs.cpwaterfight.scoreboard;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.game.PlayerProgress;
import de.codingplugs.cpwaterfight.game.RankedProgressEntry;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.level.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Sidebar scoreboards for players in Water Fight arenas.
 */
public final class ScoreboardManager {

    private static final String ENABLED_PATH = "scoreboard.enabled";
    private static final String INTERVAL_PATH = "scoreboard.update-interval-ticks";
    private static final String TITLE_PATH = "scoreboard.title";
    private static final String LINES_PATH = "scoreboard.lines";
    private static final String OBJECTIVE_NAME = "cpwf";
    private static final String TEAM_PREFIX = "cpwf_line_";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final GameManager gameManager;
    private final LevelManager levelManager;

    private JoinManager joinManager;
    private BukkitTask updateTask;
    private final Set<UUID> managedPlayers = new HashSet<>();

    public ScoreboardManager(
            JavaPlugin plugin,
            ConfigManager configManager,
            GameManager gameManager,
            LevelManager levelManager
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
        this.levelManager = Objects.requireNonNull(levelManager, "levelManager");
    }

    public void setJoinManager(JoinManager joinManager) {
        this.joinManager = joinManager;
    }

    public void load() {
        shutdownTask();
        if (isEnabled()) {
            startUpdateTask();
        }
    }

    public void reload() {
        load();
        refreshAllManaged();
    }

    public void shutdown() {
        shutdownTask();
        for (UUID playerId : new HashSet<>(managedPlayers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                resetPlayerScoreboard(player);
            }
        }
        managedPlayers.clear();
    }

    public void show(Player player) {
        if (player == null || !isEnabled()) {
            return;
        }

        managedPlayers.add(player.getUniqueId());
        joinManager.getArena(player).ifPresent(arena -> updatePlayer(player, arena));
    }

    public void remove(Player player) {
        if (player == null) {
            return;
        }

        managedPlayers.remove(player.getUniqueId());
        if (player.isOnline()) {
            resetPlayerScoreboard(player);
        }
    }

    public void updateArena(Arena arena) {
        if (arena == null || !isEnabled() || joinManager == null) {
            return;
        }

        for (Player player : getOnlineArenaPlayers(arena)) {
            if (managedPlayers.contains(player.getUniqueId())) {
                updatePlayer(player, arena);
            }
        }
    }

    public void refreshAllManaged() {
        if (!isEnabled() || joinManager == null) {
            return;
        }

        for (UUID playerId : new HashSet<>(managedPlayers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                managedPlayers.remove(playerId);
                continue;
            }

            joinManager.getArena(player).ifPresentOrElse(
                    arena -> updatePlayer(player, arena),
                    () -> remove(player)
            );
        }
    }

    private void startUpdateTask() {
        long interval = Math.max(1L, configManager.config().getLong(INTERVAL_PATH, 20L));
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAllManaged, interval, interval);
    }

    private void shutdownTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private boolean isEnabled() {
        return configManager.config().getBoolean(ENABLED_PATH, true);
    }

    private void updatePlayer(Player player, Arena arena) {
        if (player == null || arena == null || !player.isOnline()) {
            return;
        }

        List<String> templateLines = configManager.config().getStringList(LINES_PATH);
        if (templateLines.isEmpty()) {
            return;
        }

        Map<String, String> placeholders = buildPlaceholders(player, arena);
        List<String> renderedLines = new ArrayList<>(templateLines.size());
        for (String template : templateLines) {
            renderedLines.add(applyPlaceholders(template, placeholders));
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        String title = colorize(applyPlaceholders(
                configManager.config().getString(TITLE_PATH, "&b&lWater Fight"),
                placeholders
        ));

        Objective objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int scoreValue = renderedLines.size();
        for (int lineIndex = 0; lineIndex < renderedLines.size(); lineIndex++) {
            String coloredLine = colorize(renderedLines.get(lineIndex));
            String teamId = TEAM_PREFIX + lineIndex;
            ScoreboardLineHelper.applyLine(scoreboard, teamId, lineIndex, coloredLine);

            String entry = ScoreboardLineHelper.uniqueEntry(lineIndex);
            objective.getScore(entry).setScore(scoreValue--);
        }

        player.setScoreboard(scoreboard);
    }

    private Map<String, String> buildPlaceholders(Player player, Arena arena) {
        Map<String, String> values = new HashMap<>();
        UUID playerId = player.getUniqueId();

        PlayerProgress progress = resolveProgress(arena, playerId);
        int level = progress.getLevel();
        int maxLevel = levelManager.getMaxLevel();
        int killsRequired = levelManager.getKillsRequired(level);

        values.put("map", arena.displayName());
        values.put("level", String.valueOf(level));
        values.put("max_level", String.valueOf(maxLevel));
        values.put("weapon", weaponNameForLevel(level));
        values.put("kills", String.valueOf(progress.getKillsOnCurrentLevel()));
        values.put("kills_required", String.valueOf(killsRequired));
        values.put("total_kills", String.valueOf(progress.getTotalKills()));
        values.put("rank", String.valueOf(resolveRank(arena, playerId)));

        List<RankedProgressEntry> ranked = gameManager.getRankedProgress(arena);
        for (int slot = 1; slot <= 3; slot++) {
            applyTopPlaceholders(values, ranked, slot);
        }

        return values;
    }

    private PlayerProgress resolveProgress(Arena arena, UUID playerId) {
        return gameManager.getProgress(arena, playerId)
                .orElseGet(() -> lobbyProgress(playerId));
    }

    private static PlayerProgress lobbyProgress(UUID playerId) {
        PlayerProgress progress = new PlayerProgress(playerId);
        progress.reset();
        return progress;
    }

    private int resolveRank(Arena arena, UUID playerId) {
        List<RankedProgressEntry> ranked = gameManager.getRankedProgress(arena);
        for (int index = 0; index < ranked.size(); index++) {
            if (ranked.get(index).playerId().equals(playerId)) {
                return index + 1;
            }
        }
        return ranked.size() + 1;
    }

    private void applyTopPlaceholders(Map<String, String> values, List<RankedProgressEntry> ranked, int slot) {
        String prefix = "top_" + slot + "_";
        if (ranked.size() < slot) {
            values.put(prefix + "name", "-");
            values.put(prefix + "level", "-");
            values.put(prefix + "kills", "-");
            return;
        }

        RankedProgressEntry entry = ranked.get(slot - 1);
        values.put(prefix + "name", resolvePlayerName(entry));
        values.put(prefix + "level", String.valueOf(entry.progress().getLevel()));
        values.put(prefix + "kills", String.valueOf(entry.progress().getTotalKills()));
    }

    private String resolvePlayerName(RankedProgressEntry entry) {
        if (entry.player() != null) {
            return entry.player().getName();
        }

        String offlineName = Bukkit.getOfflinePlayer(entry.playerId()).getName();
        return offlineName != null ? offlineName : "-";
    }

    private String weaponNameForLevel(int level) {
        return levelManager.getLevel(level)
                .map(definition -> stripColor(definition.weapon().displayName()))
                .orElse("-");
    }

    private List<Player> getOnlineArenaPlayers(Arena arena) {
        if (joinManager == null) {
            return List.of();
        }

        List<Player> players = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            joinManager.getArena(online)
                    .filter(current -> current.id().equals(arena.id()))
                    .ifPresent(ignored -> players.add(online));
        }
        return players;
    }

    private static void resetPlayerScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private static String applyPlaceholders(String template, Map<String, String> placeholders) {
        if (template == null) {
            return "";
        }

        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    private static String colorize(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private static String stripColor(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }
}
