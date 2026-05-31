package de.codingplugs.cpwaterfight.protection;

import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles protection feedback messages per player without affecting protection logic.
 */
public final class ProtectionMessageThrottler {

    public static final String KEY_ACTION_BLOCKED = "protection.action-blocked";
    public static final String KEY_COMMAND_BLOCKED = "protection.command-blocked";

    private final MessageManager messages;
    private final ProtectionSettings protectionSettings;
    private final Map<UUID, Map<String, Long>> lastSentMillis = new ConcurrentHashMap<>();

    public ProtectionMessageThrottler(MessageManager messages, ProtectionSettings protectionSettings) {
        this.messages = messages;
        this.protectionSettings = protectionSettings;
    }

    public void sendActionBlocked(Player player) {
        sendThrottled(player, KEY_ACTION_BLOCKED);
    }

    public void sendCommandBlocked(Player player) {
        sendThrottled(player, KEY_COMMAND_BLOCKED);
    }

    public void forget(Player player) {
        if (player != null) {
            lastSentMillis.remove(player.getUniqueId());
        }
    }

    private void sendThrottled(Player player, String messageKey) {
        if (player == null || messageKey == null || messageKey.isBlank()) {
            return;
        }

        long cooldownMillis = protectionSettings.messageCooldownMillis();
        if (cooldownMillis <= 0) {
            messages.sendPrefixed(player, messageKey);
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Map<String, Long> perMessage = lastSentMillis.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());

        Long lastSent = perMessage.get(messageKey);
        if (lastSent != null && now - lastSent < cooldownMillis) {
            return;
        }

        perMessage.put(messageKey, now);
        messages.sendPrefixed(player, messageKey);
    }
}
