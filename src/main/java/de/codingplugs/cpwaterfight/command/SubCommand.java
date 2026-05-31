package de.codingplugs.cpwaterfight.command;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * A single Water Fight command subcommand.
 */
public interface SubCommand {

    String name();

    String permission();

    boolean execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
