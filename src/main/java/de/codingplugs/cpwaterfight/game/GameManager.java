package de.codingplugs.cpwaterfight.game;

/**
 * Coordinates active Water Fight matches. Gameplay will be implemented in a later step.
 */
public final class GameManager {

    private GameState state = GameState.WAITING;

    public GameManager() {
    }

    public void load() {
        state = GameState.WAITING;
    }

    public void reload() {
        load();
    }

    public void shutdown() {
        state = GameState.WAITING;
    }

    public GameState state() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }
}
