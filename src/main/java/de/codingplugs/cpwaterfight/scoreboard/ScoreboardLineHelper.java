package de.codingplugs.cpwaterfight.scoreboard;

import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Builds unique sidebar line entries and applies colored text via scoreboard teams.
 */
final class ScoreboardLineHelper {

    private static final int MAX_TEAM_TEXT = 64;

    private ScoreboardLineHelper() {
    }

    static String uniqueEntry(int lineIndex) {
        int first = lineIndex % 16;
        int second = (lineIndex / 16) % 16;
        int third = (lineIndex / 256) % 16;
        return ChatColor.values()[first].toString()
                + ChatColor.values()[second]
                + ChatColor.values()[third]
                + ChatColor.RESET;
    }

    static void applyLine(Scoreboard scoreboard, String teamId, int lineIndex, String coloredText) {
        Team team = scoreboard.getTeam(teamId);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamId);
        } else {
            for (String entry : team.getEntries()) {
                team.removeEntry(entry);
            }
        }

        String entry = uniqueEntry(lineIndex);
        team.addEntry(entry);
        setTeamText(team, coloredText);
    }

    private static void setTeamText(Team team, String coloredText) {
        team.setPrefix("");
        team.setSuffix("");

        if (coloredText == null || coloredText.isEmpty()) {
            return;
        }

        if (coloredText.length() <= MAX_TEAM_TEXT) {
            team.setPrefix(coloredText);
            return;
        }

        String prefix = coloredText.substring(0, MAX_TEAM_TEXT);
        String suffix = coloredText.substring(MAX_TEAM_TEXT);
        if (suffix.length() > MAX_TEAM_TEXT) {
            suffix = suffix.substring(0, MAX_TEAM_TEXT);
        }

        team.setPrefix(prefix);
        team.setSuffix(suffix);
    }
}
