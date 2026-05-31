package de.codingplugs.cpwaterfight.listener;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.command.CommandPermissions;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.game.GameState;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import de.codingplugs.cpwaterfight.protection.ProtectionMessageThrottler;
import de.codingplugs.cpwaterfight.protection.ProtectionSettings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import java.util.Locale;
import java.util.Set;

/**
 * Prevents griefing and inventory abuse for players in Water Fight arenas.
 */
public final class GameProtectionListener implements Listener {

    private final JoinManager joinManager;
    private final GameManager gameManager;
    private final ProtectionSettings protection;
    private final ProtectionMessageThrottler messageThrottler;

    public GameProtectionListener(
            JoinManager joinManager,
            GameManager gameManager,
            MessageManager messages,
            ProtectionSettings protection
    ) {
        this.joinManager = joinManager;
        this.gameManager = gameManager;
        this.protection = protection;
        this.messageThrottler = new ProtectionMessageThrottler(messages, protection);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!protection.blockItemDrop() || !isJoined(event.getPlayer())) {
            return;
        }

        cancelWithActionMessage(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!protection.blockItemPickup() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!isJoined(player)) {
            return;
        }

        cancelWithActionMessage(player, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!protection.blockInventoryClick() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!isJoined(player)) {
            return;
        }

        cancelWithActionMessage(player, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!protection.blockInventoryClick() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!isJoined(player)) {
            return;
        }

        cancelWithActionMessage(player, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!protection.blockOffhandSwap() || !isJoined(event.getPlayer())) {
            return;
        }

        cancelWithActionMessage(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!protection.blockBreak() || !isJoined(event.getPlayer())) {
            return;
        }

        cancelWithActionMessage(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!protection.blockPlace() || !isJoined(event.getPlayer())) {
            return;
        }

        cancelWithActionMessage(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!protection.lockFoodLevel() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!isJoined(player)) {
            return;
        }

        event.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (!protection.preventItemDurabilityLoss() || !isJoined(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Arena arena = joinManager.getArena(player).orElse(null);
        if (arena == null) {
            return;
        }

        GameState state = gameManager.getArenaState(arena);
        if (state != GameState.WAITING && state != GameState.COUNTDOWN) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (isFallDamage(cause) && gameManager.isFallDamageProtectionEnabled()) {
            event.setCancelled(true);
            return;
        }

        if (isFireDamage(cause) && protection.preventFireDamageBeforeStart()) {
            event.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.DROWNING && protection.preventDrowningBeforeStart()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!protection.restrictCommandsIngame()) {
            return;
        }

        Player player = event.getPlayer();
        if (canBypassCommandRestriction(player)) {
            return;
        }

        Arena arena = joinManager.getArena(player).orElse(null);
        if (arena == null) {
            return;
        }

        GameState state = gameManager.getArenaState(arena);
        if (state != GameState.INGAME && state != GameState.ENDING) {
            return;
        }

        String rootCommand = parseRootCommand(event.getMessage());
        if (rootCommand.isEmpty() || isCommandAllowed(rootCommand)) {
            return;
        }

        event.setCancelled(true);
        messageThrottler.sendCommandBlocked(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        messageThrottler.forget(event.getPlayer());
    }

    private boolean isJoined(Player player) {
        return player != null && joinManager.isInArena(player);
    }

    private boolean canBypassCommandRestriction(Player player) {
        return player.isOp() || player.hasPermission(CommandPermissions.ADMIN);
    }

    private boolean isCommandAllowed(String rootCommand) {
        Set<String> allowed = protection.allowedCommandsIngame();
        return allowed.contains(rootCommand);
    }

    private static String parseRootCommand(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }

        String trimmed = message.stripLeading();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        int spaceIndex = trimmed.indexOf(' ');
        String commandToken = spaceIndex >= 0 ? trimmed.substring(0, spaceIndex) : trimmed;
        if (commandToken.isEmpty()) {
            return "";
        }

        int namespaceIndex = commandToken.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex < commandToken.length() - 1) {
            commandToken = commandToken.substring(namespaceIndex + 1);
        }

        return commandToken.toLowerCase(Locale.ROOT);
    }

    private static boolean isFallDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.FALL;
    }

    private static boolean isFireDamage(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, CAMPFIRE -> true;
            default -> false;
        };
    }

    private void cancelWithActionMessage(Player player, org.bukkit.event.Cancellable event) {
        event.setCancelled(true);
        messageThrottler.sendActionBlocked(player);
    }
}
