package de.codingplugs.cpwaterfight.command.join;

import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class LeaveSubCommand extends JoinPlayerSubCommand {

    private final JoinManager joinManager;

    public LeaveSubCommand(MessageManager messages, JoinManager joinManager) {
        super(messages);
        this.joinManager = joinManager;
    }

    @Override
    public String name() {
        return "leave";
    }

    @Override
    protected boolean executePlayer(Player player, String[] args) {
        joinManager.leave(player);
        return true;
    }

    @Override
    public List<String> tabComplete(org.bukkit.command.CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
