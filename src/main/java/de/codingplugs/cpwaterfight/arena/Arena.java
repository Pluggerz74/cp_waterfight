package de.codingplugs.cpwaterfight.arena;

import de.codingplugs.cpwaterfight.game.GameState;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A configured Water Fight arena.
 */
public final class Arena {

    private final String id;
    private String displayName;
    private String worldName;
    private Location lobby;
    private Location joinBlock;
    private final List<Location> spawns;
    private int minPlayers;
    private int maxPlayers;
    private GameState state;

    public Arena(
            String id,
            String displayName,
            String worldName,
            Location lobby,
            Location joinBlock,
            List<Location> spawns,
            int minPlayers,
            int maxPlayers,
            GameState state
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = displayName != null ? displayName : id;
        this.worldName = worldName;
        this.lobby = lobby;
        this.joinBlock = joinBlock;
        this.spawns = new ArrayList<>(spawns != null ? spawns : List.of());
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.state = state != null ? state : GameState.WAITING;
    }

    public static Arena createDefault(String id, int minPlayers, int maxPlayers) {
        return new Arena(id, formatDisplayName(id), null, null, null, List.of(), minPlayers, maxPlayers, GameState.WAITING);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String worldName() {
        return worldName;
    }

    public Location lobby() {
        return cloneLocation(lobby);
    }

    public void setLobby(Location lobby) {
        this.lobby = cloneLocation(lobby);
        if (lobby != null && lobby.getWorld() != null) {
            this.worldName = lobby.getWorld().getName();
        }
    }

    public boolean hasLobby() {
        return lobby != null;
    }

    public Location joinBlock() {
        return cloneLocation(joinBlock);
    }

    public void setJoinBlock(Location joinBlock) {
        this.joinBlock = cloneLocation(joinBlock);
        if (this.joinBlock != null && this.joinBlock.getWorld() != null && worldName == null) {
            this.worldName = this.joinBlock.getWorld().getName();
        }
    }

    public boolean hasJoinBlock() {
        return joinBlock != null;
    }

    public List<Location> spawns() {
        return Collections.unmodifiableList(spawns.stream().map(Arena::cloneLocation).toList());
    }

    public int spawnCount() {
        return spawns.size();
    }

    public void addSpawn(Location spawn) {
        Location copy = cloneLocation(spawn);
        if (copy != null) {
            spawns.add(copy);
            if (copy.getWorld() != null && worldName == null) {
                worldName = copy.getWorld().getName();
            }
        }
    }

    public int minPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public GameState state() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    private static String formatDisplayName(String id) {
        if (id == null || id.isBlank()) {
            return id;
        }
        return id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
    }

    private static Location cloneLocation(Location location) {
        return location != null ? location.clone() : null;
    }

}
