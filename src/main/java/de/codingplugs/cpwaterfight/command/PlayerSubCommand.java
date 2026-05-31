package de.codingplugs.cpwaterfight.command;

import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class PlayerSubCommand extends AdminSubCommand {

    protected PlayerSubCommand(MessageManager messages) {
        super(messages);
    }

    @Override
    public final boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.sendPrefixed(sender, "general.player-only");
            return true;
        }

        return executePlayer(player, args);
    }

    protected abstract boolean executePlayer(Player player, String[] args);
}
