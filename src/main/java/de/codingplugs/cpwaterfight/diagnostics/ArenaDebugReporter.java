package de.codingplugs.cpwaterfight.diagnostics;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.arena.ArenaValidationResult;
import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.game.GameSession;
import de.codingplugs.cpwaterfight.game.GameState;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.level.LevelManager;
import de.codingplugs.cpwaterfight.protection.ProtectionSettings;
import de.codingplugs.cpwaterfight.spectator.SpectatorManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Collects read-only diagnostic data for an arena debug report.
 */
public final class ArenaDebugReporter {

    private ArenaDebugReporter() {
    }

    public static Map<String, String> collect(
            Arena arena,
            JoinManager joinManager,
            GameManager gameManager,
            ArenaManager arenaManager,
            LevelManager levelManager,
            ConfigManager configManager,
            ProtectionSettings protectionSettings,
            SpectatorManager spectatorManager
    ) {
        Map<String, String> lines = new LinkedHashMap<>();
        if (arena == null) {
            return lines;
        }

        GameState state = gameManager.getArenaState(arena);
        Optional<GameSession> session = gameManager.getSession(arena);
        ArenaValidationResult validation = arenaManager.validateArena(arena);

        lines.put("Arena ID", arena.id());
        lines.put("Display name", arena.displayName());
        lines.put("State", state.name());
        lines.put("Joined players", String.valueOf(joinManager.getPlayerCount(arena)));
        lines.put("Session players", String.valueOf(session.map(GameSession::getPlayerCount).orElse(0)));
        lines.put("Session online", String.valueOf(countOnlineSessionPlayers(session)));
        lines.put("Min / max players", arena.minPlayers() + " / " + arena.maxPlayers());
        lines.put("Lobby set", yesNo(arena.hasLobby()));
        lines.put("Join block set", yesNo(arena.hasJoinBlock()));
        lines.put("Spawn count", String.valueOf(arena.spawnCount()));
        lines.put("Countdown running", yesNo(session.map(GameSession::isCountdownRunning).orElse(false)));
        lines.put("Countdown seconds", formatCountdownSeconds(session));
        lines.put("Scoreboard enabled", yesNo(configManager.config().getBoolean("scoreboard.enabled", true)));
        lines.put("Protections", protectionSettings.summary());
        lines.put("Spectating", String.valueOf(spectatorManager != null ? spectatorManager.getSpectatingCount(arena) : 0));
        lines.put("Max level", String.valueOf(levelManager.getMaxLevel()));
        lines.put("Default kills required", String.valueOf(levelManager.getDefaultKillsRequired()));
        lines.put("Setup ready", yesNo(validation.ready()));

        return Map.copyOf(lines);
    }

    public static List<Map.Entry<String, String>> asOrderedLines(Map<String, String> data) {
        return List.copyOf(data.entrySet());
    }

    private static int countOnlineSessionPlayers(Optional<GameSession> session) {
        if (session.isEmpty()) {
            return 0;
        }

        int online = 0;
        for (UUID playerId : session.get().getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                online++;
            }
        }
        return online;
    }

    private static String formatCountdownSeconds(Optional<GameSession> session) {
        if (session.isEmpty() || !session.get().isCountdownRunning()) {
            return "-";
        }
        return String.valueOf(session.get().getCountdownSecondsRemaining());
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
