package de.codingplugs.cpwaterfight.join;

/**
 * Handles player join flow into Water Fight. Join-head logic comes in a later step.
 */
public final class JoinManager {

    public JoinManager() {
    }

    public void load() {
        // Join flow will be wired when lobby integration is implemented.
    }

    public void reload() {
        load();
    }

    public void shutdown() {
        // Reserved for future queue and session cleanup.
    }
}
