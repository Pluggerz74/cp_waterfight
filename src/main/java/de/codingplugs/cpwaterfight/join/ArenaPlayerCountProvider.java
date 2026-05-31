package de.codingplugs.cpwaterfight.join;

import de.codingplugs.cpwaterfight.arena.Arena;

/**
 * Supplies live joined player counts for arena displays.
 */
@FunctionalInterface
public interface ArenaPlayerCountProvider {

    int getPlayerCount(Arena arena);
}
