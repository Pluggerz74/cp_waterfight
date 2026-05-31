package de.codingplugs.cpwaterfight.arena;

import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of validating whether an arena is ready for Water Fight matches.
 */
public final class ArenaValidationResult {

    private final boolean ready;
    private final List<ArenaValidationEntry> entries;
    private final List<String> issues;

    public ArenaValidationResult(boolean ready, List<ArenaValidationEntry> entries) {
        this.ready = ready;
        this.entries = List.copyOf(entries != null ? entries : List.of());
        this.issues = new ArrayList<>();
        for (ArenaValidationEntry entry : this.entries) {
            if (!entry.valid()) {
                issues.add(entry.label());
            }
        }
    }

    public boolean ready() {
        return ready;
    }

    public List<ArenaValidationEntry> entries() {
        return entries;
    }

    public List<String> issues() {
        return List.copyOf(issues);
    }
}
