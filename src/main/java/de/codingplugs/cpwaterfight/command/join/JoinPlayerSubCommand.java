package de.codingplugs.cpwaterfight.command.join;

import de.codingplugs.cpwaterfight.command.CommandPermissions;
import de.codingplugs.cpwaterfight.command.SubCommand;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class JoinPlayerSubCommand implements SubCommand {

    protected final MessageManager messages;

    protected JoinPlayerSubCommand(MessageManager messages) {
        this.messages = messages;
    }

    @Override
    public String permission() {
        return CommandPermissions.JOIN;
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
