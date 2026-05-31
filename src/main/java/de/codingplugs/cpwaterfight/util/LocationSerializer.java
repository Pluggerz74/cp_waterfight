package de.codingplugs.cpwaterfight.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializes and deserializes Bukkit locations to YAML configuration sections.
 */
public final class LocationSerializer {

    private static final String WORLD = "world";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";
    private static final String YAW = "yaw";
    private static final String PITCH = "pitch";

    private LocationSerializer() {
    }

    public static void write(ConfigurationSection section, Location location) {
        write(section, location, true);
    }

    public static void writeBlock(ConfigurationSection section, Location location) {
        write(section, location, false);
    }

    public static void write(ConfigurationSection section, Location location, boolean includeRotation) {
        if (section == null || location == null || location.getWorld() == null) {
            return;
        }

        section.set(WORLD, location.getWorld().getName());
        section.set(X, location.getX());
        section.set(Y, location.getY());
        section.set(Z, location.getZ());

        if (includeRotation) {
            section.set(YAW, location.getYaw());
            section.set(PITCH, location.getPitch());
        } else {
            section.set(YAW, null);
            section.set(PITCH, null);
        }
    }

    public static Location read(ConfigurationSection section) {
        return read(section, true);
    }

    public static Location readBlock(ConfigurationSection section) {
        Location location = read(section, false);
        if (location == null) {
            return null;
        }

        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        return new Location(
                world,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public static Location read(ConfigurationSection section, boolean includeRotation) {
        if (section == null || !section.contains(WORLD)) {
            return null;
        }

        String worldName = section.getString(WORLD);
        if (worldName == null || worldName.isBlank()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        if (!section.contains(X) || !section.contains(Y) || !section.contains(Z)) {
            return null;
        }

        double x = section.getDouble(X);
        double y = section.getDouble(Y);
        double z = section.getDouble(Z);

        if (includeRotation && section.contains(YAW) && section.contains(PITCH)) {
            return new Location(world, x, y, z, (float) section.getDouble(YAW), (float) section.getDouble(PITCH));
        }

        return new Location(world, x, y, z);
    }

    public static Map<String, Object> toMap(Location location, boolean includeRotation) {
        if (location == null || location.getWorld() == null) {
            return Map.of();
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(WORLD, location.getWorld().getName());
        map.put(X, location.getX());
        map.put(Y, location.getY());
        map.put(Z, location.getZ());

        if (includeRotation) {
            map.put(YAW, location.getYaw());
            map.put(PITCH, location.getPitch());
        }

        return map;
    }

    public static Location fromMap(Map<?, ?> map, boolean includeRotation) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        Object worldValue = map.get(WORLD);
        if (!(worldValue instanceof String worldName) || worldName.isBlank()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        if (!map.containsKey(X) || !map.containsKey(Y) || !map.containsKey(Z)) {
            return null;
        }

        double x = toDouble(map.get(X));
        double y = toDouble(map.get(Y));
        double z = toDouble(map.get(Z));

        if (includeRotation && map.containsKey(YAW) && map.containsKey(PITCH)) {
            return new Location(world, x, y, z, (float) toDouble(map.get(YAW)), (float) toDouble(map.get(PITCH)));
        }

        return new Location(world, x, y, z);
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0D;
    }

    public static boolean isSameBlock(Location first, Location second) {
        if (first == null || second == null) {
            return false;
        }

        World firstWorld = first.getWorld();
        World secondWorld = second.getWorld();
        if (firstWorld == null || secondWorld == null) {
            return false;
        }

        return firstWorld.getUID().equals(secondWorld.getUID())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
