package de.codingplugs.cpwaterfight.arena;

import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.game.GameState;
import de.codingplugs.cpwaterfight.util.LocationSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads, persists, and manages Water Fight arenas.
 */
public final class ArenaManager {

    private static final String ROOT_PATH = "arenas";

    private final ConfigManager configManager;
    private final Logger logger;

    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaManager(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    public void load() {
        arenas.clear();
        FileConfiguration config = configManager.arenas();
        ConfigurationSection root = config.getConfigurationSection(ROOT_PATH);
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            try {
                String normalizedId = id.toLowerCase(Locale.ROOT);
                arenas.put(normalizedId, deserializeArena(normalizedId, section));
            } catch (Exception exception) {
                logger.log(Level.WARNING, "Could not load arena '" + id + "'", exception);
            }
        }
    }

    public void reload() {
        load();
    }

    public void shutdown() {
        save();
    }

    public void save() {
        FileConfiguration config = configManager.arenas();
        config.set(ROOT_PATH, null);

        ConfigurationSection root = config.createSection(ROOT_PATH);
        for (Arena arena : arenas.values()) {
            serializeArena(root.createSection(arena.id()), arena);
        }

        configManager.save(ConfigManager.ARENAS_FILE, config);
    }

    public Optional<Arena> createArena(String id) {
        String normalizedId = normalizeId(id);
        if (normalizedId == null) {
            return Optional.empty();
        }

        if (arenas.containsKey(normalizedId)) {
            return Optional.empty();
        }

        int minPlayers = configManager.config().getInt("min-players", 2);
        int maxPlayers = configManager.config().getInt("max-players", 16);

        Arena arena = Arena.createDefault(normalizedId, minPlayers, maxPlayers);
        arenas.put(normalizedId, arena);
        save();
        return Optional.of(arena);
    }

    public boolean deleteArena(String id) {
        String normalizedId = normalizeId(id);
        if (normalizedId == null || !arenas.containsKey(normalizedId)) {
            return false;
        }

        arenas.remove(normalizedId);
        save();
        return true;
    }

    public Optional<Arena> getArena(String id) {
        String normalizedId = normalizeId(id);
        if (normalizedId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(arenas.get(normalizedId));
    }

    public Collection<Arena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public List<String> getArenaIds() {
        return List.copyOf(arenas.keySet());
    }

    public boolean setLobby(String id, Location location) {
        return mutateArena(id, arena -> arena.setLobby(location));
    }

    public boolean setJoinBlock(String id, Location location) {
        return mutateArena(id, arena -> arena.setJoinBlock(location));
    }

    public boolean addSpawn(String id, Location location) {
        return mutateArena(id, arena -> arena.addSpawn(location));
    }

    public ArenaCapacityResult setMinPlayers(String id, int minPlayers) {
        Optional<Arena> arenaOptional = getArena(id);
        if (arenaOptional.isEmpty()) {
            return ArenaCapacityResult.ARENA_NOT_FOUND;
        }

        int clamped = Math.max(1, minPlayers);
        Arena arena = arenaOptional.get();
        if (clamped > arena.maxPlayers()) {
            return ArenaCapacityResult.MIN_GREATER_THAN_MAX;
        }

        arena.setMinPlayers(clamped);
        save();
        return ArenaCapacityResult.SUCCESS;
    }

    public ArenaCapacityResult setMaxPlayers(String id, int maxPlayers) {
        Optional<Arena> arenaOptional = getArena(id);
        if (arenaOptional.isEmpty()) {
            return ArenaCapacityResult.ARENA_NOT_FOUND;
        }

        int clamped = Math.max(1, maxPlayers);
        Arena arena = arenaOptional.get();
        if (clamped < arena.minPlayers()) {
            return ArenaCapacityResult.MAX_LESS_THAN_MIN;
        }

        arena.setMaxPlayers(clamped);
        save();
        return ArenaCapacityResult.SUCCESS;
    }

    public boolean renameArena(String id, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return false;
        }

        Optional<Arena> arenaOptional = getArena(id);
        if (arenaOptional.isEmpty()) {
            return false;
        }

        arenaOptional.get().setDisplayName(displayName.trim());
        save();
        return true;
    }

    public ArenaValidationResult validateArena(Arena arena) {
        if (arena == null) {
            return new ArenaValidationResult(false, List.of(
                    new ArenaValidationEntry("Arena exists", false)
            ));
        }

        List<ArenaValidationEntry> entries = new ArrayList<>();
        entries.add(new ArenaValidationEntry("Arena exists", true));
        entries.add(new ArenaValidationEntry("Lobby configured", arena.hasLobby()));
        entries.add(new ArenaValidationEntry("Join block configured", arena.hasJoinBlock()));
        entries.add(new ArenaValidationEntry("At least one spawn", arena.spawnCount() >= 1));
        entries.add(new ArenaValidationEntry("Minimum players >= 1", arena.minPlayers() >= 1));
        entries.add(new ArenaValidationEntry("Maximum players > 0", arena.maxPlayers() > 0));
        entries.add(new ArenaValidationEntry("Capacity valid (max >= min)", arena.maxPlayers() >= arena.minPlayers()));
        entries.add(new ArenaValidationEntry("World reference valid", isWorldReferenceValid(arena)));
        entries.add(new ArenaValidationEntry("Lobby world loaded", isLobbyWorldValid(arena)));
        entries.add(new ArenaValidationEntry("Join block world loaded", isJoinBlockWorldValid(arena)));
        entries.add(new ArenaValidationEntry("Spawn worlds loaded", areSpawnWorldsValid(arena)));

        boolean ready = entries.stream().allMatch(ArenaValidationEntry::valid);
        return new ArenaValidationResult(ready, entries);
    }

    public Optional<Arena> findByJoinBlock(Location blockLocation) {
        if (blockLocation == null) {
            return Optional.empty();
        }

        for (Arena arena : arenas.values()) {
            Location joinBlock = arena.joinBlock();
            if (joinBlock != null && LocationSerializer.isSameBlock(joinBlock, blockLocation)) {
                return Optional.of(arena);
            }
        }

        return Optional.empty();
    }

    public boolean isValidId(String id) {
        return normalizeId(id) != null;
    }

    public boolean exists(String id) {
        String normalizedId = normalizeId(id);
        return normalizedId != null && arenas.containsKey(normalizedId);
    }

    private boolean mutateArena(String id, ArenaMutation mutation) {
        Optional<Arena> arenaOptional = getArena(id);
        if (arenaOptional.isEmpty()) {
            return false;
        }

        mutation.apply(arenaOptional.get());
        save();
        return true;
    }

    private Arena deserializeArena(String id, ConfigurationSection section) {
        String displayName = section.getString("display-name", id);
        int minPlayers = section.getInt("min-players", configManager.config().getInt("min-players", 2));
        int maxPlayers = section.getInt("max-players", configManager.config().getInt("max-players", 16));

        Location lobby = LocationSerializer.read(section.getConfigurationSection("lobby"));
        Location joinBlock = LocationSerializer.readBlock(section.getConfigurationSection("join-block"));

        List<Location> spawns = new ArrayList<>();
        for (Map<?, ?> spawnMap : section.getMapList("spawns")) {
            Location spawn = LocationSerializer.fromMap(spawnMap, true);
            if (spawn != null) {
                spawns.add(spawn);
            }
        }

        String worldName = section.getString("world");
        if (worldName == null && lobby != null && lobby.getWorld() != null) {
            worldName = lobby.getWorld().getName();
        }

        GameState state = GameState.WAITING;
        String stateName = section.getString("state");
        if (stateName != null) {
            try {
                state = GameState.valueOf(stateName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                logger.warning("Invalid state for arena '" + id + "': " + stateName);
            }
        }

        return new Arena(id, displayName, worldName, lobby, joinBlock, spawns, minPlayers, maxPlayers, state);
    }

    private void serializeArena(ConfigurationSection section, Arena arena) {
        section.set("display-name", arena.displayName());
        section.set("min-players", arena.minPlayers());
        section.set("max-players", arena.maxPlayers());
        section.set("state", arena.state().name());

        if (arena.worldName() != null) {
            section.set("world", arena.worldName());
        }

        if (arena.hasLobby()) {
            LocationSerializer.write(section.createSection("lobby"), arena.lobby());
        } else {
            section.set("lobby", null);
        }

        if (arena.hasJoinBlock()) {
            LocationSerializer.writeBlock(section.createSection("join-block"), arena.joinBlock());
        } else {
            section.set("join-block", null);
        }

        List<Map<String, Object>> spawnMaps = new ArrayList<>();
        for (Location spawn : arena.spawns()) {
            spawnMaps.add(LocationSerializer.toMap(spawn, true));
        }
        section.set("spawns", spawnMaps.isEmpty() ? null : spawnMaps);
    }

    private static boolean isWorldReferenceValid(Arena arena) {
        String worldName = arena.worldName();
        if (worldName == null || worldName.isBlank()) {
            return arena.hasLobby() || arena.hasJoinBlock() || arena.spawnCount() > 0;
        }
        return Bukkit.getWorld(worldName) != null;
    }

    private static boolean isLobbyWorldValid(Arena arena) {
        if (!arena.hasLobby()) {
            return false;
        }
        Location lobby = arena.lobby();
        return lobby != null && lobby.getWorld() != null;
    }

    private static boolean isJoinBlockWorldValid(Arena arena) {
        if (!arena.hasJoinBlock()) {
            return false;
        }
        Location joinBlock = arena.joinBlock();
        return joinBlock != null && joinBlock.getWorld() != null;
    }

    private static boolean areSpawnWorldsValid(Arena arena) {
        if (arena.spawnCount() == 0) {
            return false;
        }

        for (Location spawn : arena.spawns()) {
            if (spawn == null || spawn.getWorld() == null) {
                return false;
            }
            World world = spawn.getWorld();
            if (Bukkit.getWorld(world.getUID()) == null && Bukkit.getWorld(world.getName()) == null) {
                return false;
            }
        }
        return true;
    }

    private String normalizeId(String id) {
        if (id == null) {
            return null;
        }

        String normalized = id.toLowerCase(Locale.ROOT).trim();
        if (normalized.isEmpty() || !normalized.matches("[a-z0-9_-]+")) {
            return null;
        }

        return normalized;
    }

    @FunctionalInterface
    private interface ArenaMutation {
        void apply(Arena arena);
    }
}
