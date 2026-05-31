package de.codingplugs.cpwaterfight.display;

import de.codingplugs.cpwaterfight.CPWaterFight;
import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.game.GameState;
import de.codingplugs.cpwaterfight.game.GameStateLabels;
import de.codingplugs.cpwaterfight.message.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spawns and maintains TextDisplay entities above arena join blocks.
 */
public final class JoinDisplayManager {

    private static final String CONFIG_ROOT = "join-display";

    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final ArenaManager arenaManager;
    private GameManager gameManager;
    private final GameStateLabels stateLabels;

    private final Map<String, UUID> displayIds = new HashMap<>();

    public JoinDisplayManager(
            ConfigManager configManager,
            MessageManager messageManager,
            ArenaManager arenaManager,
            GameManager gameManager,
            GameStateLabels stateLabels
    ) {
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.arenaManager = arenaManager;
        this.gameManager = gameManager;
        this.stateLabels = stateLabels;
    }

    public void attachGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void load() {
        refreshAll();
    }

    public void reload() {
        removeAll();
        refreshAll();
    }

    public void shutdown() {
        removeAll();
    }

    public void refreshAll() {
        if (!isEnabled()) {
            return;
        }

        for (Arena arena : arenaManager.getArenas()) {
            refreshArena(arena, 0);
        }
    }

    public void refreshAll(java.util.function.ToIntFunction<Arena> playerCountProvider) {
        if (!isEnabled()) {
            return;
        }

        for (Arena arena : arenaManager.getArenas()) {
            refreshArena(arena, playerCountProvider.applyAsInt(arena));
        }
    }

    public void refreshArena(Arena arena, int playerCount) {
        if (arena == null) {
            return;
        }

        removeArena(arena.id());

        if (!isEnabled() || !arena.hasJoinBlock()) {
            return;
        }

        Location spawnLocation = displayLocation(arena);
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            return;
        }

        World world = spawnLocation.getWorld();
        TextDisplay display = world.spawn(spawnLocation, TextDisplay.class, entity -> configureDisplay(entity, arena, playerCount));
        displayIds.put(arena.id(), display.getUniqueId());
    }

    public void updateArena(Arena arena, int playerCount) {
        if (arena == null) {
            return;
        }

        UUID displayId = displayIds.get(arena.id());
        if (displayId == null) {
            refreshArena(arena, playerCount);
            return;
        }

        Entity entity = Bukkit.getEntity(displayId);
        if (!(entity instanceof TextDisplay textDisplay)) {
            refreshArena(arena, playerCount);
            return;
        }

        textDisplay.text(buildText(arena, playerCount));
    }

    public void removeArena(String arenaId) {
        UUID displayId = displayIds.remove(arenaId);
        if (displayId == null) {
            return;
        }

        Entity entity = Bukkit.getEntity(displayId);
        if (entity != null) {
            entity.remove();
        }
    }

    public void removeAll() {
        for (String arenaId : List.copyOf(displayIds.keySet())) {
            removeArena(arenaId);
        }
    }

    private void configureDisplay(TextDisplay display, Arena arena, int playerCount) {
        display.text(buildText(arena, playerCount));
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(false);
        display.setDefaultBackground(false);
        display.setShadowed(true);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setPersistent(false);
        display.setInvulnerable(true);
    }

    private Location displayLocation(Arena arena) {
        Location joinBlock = arena.joinBlock();
        if (joinBlock == null || joinBlock.getWorld() == null) {
            return null;
        }

        double heightOffset = configManager.config().getDouble(CONFIG_ROOT + ".height-offset", 1.8D);
        return new Location(
                joinBlock.getWorld(),
                joinBlock.getBlockX() + 0.5D,
                joinBlock.getBlockY() + 0.5D + heightOffset,
                joinBlock.getBlockZ() + 0.5D
        );
    }

    private Component buildText(Arena arena, int playerCount) {
        List<String> lines = configManager.config().getStringList(CONFIG_ROOT + ".lines");
        if (lines.isEmpty()) {
            lines = List.of(
                    "&b&l" + CPWaterFight.GAME_MODE_NAME,
                    "&7Spieler: &a%players% &7/ &a%max_players%",
                    "&7Map: &e%map%",
                    "&7Status: &f%state%"
            );
        }

        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                combined.append('\n');
            }
            combined.append(applyPlaceholders(lines.get(i), arena, playerCount));
        }

        return messageManager.componentFromRaw(combined.toString());
    }

    private String applyPlaceholders(String line, Arena arena, int playerCount) {
        return line
                .replace("%players%", String.valueOf(playerCount))
                .replace("%max_players%", String.valueOf(arena.maxPlayers()))
                .replace("%map%", arena.displayName())
                .replace("%game%", CPWaterFight.GAME_MODE_NAME)
                .replace("%state%", stateLabels.label(
                        gameManager != null ? gameManager.getArenaState(arena) : GameState.WAITING
                ));
    }

    private boolean isEnabled() {
        return configManager.config().getBoolean(CONFIG_ROOT + ".enabled", true);
    }
}
