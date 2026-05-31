package de.codingplugs.cpwaterfight.arena;

/**
 * Result of updating arena player capacity settings.
 */
public enum ArenaCapacityResult {
    SUCCESS,
    ARENA_NOT_FOUND,
    MIN_GREATER_THAN_MAX,
    MAX_LESS_THAN_MIN
}
