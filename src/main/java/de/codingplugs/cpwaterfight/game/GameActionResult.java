package de.codingplugs.cpwaterfight.game;

/**
 * Result of an administrative or lifecycle game action.
 */
public enum GameActionResult {

    SUCCESS,
    ALREADY_RUNNING,
    NOT_RUNNING,
    NO_PLAYERS,
    MISSING_SPAWNS
}
