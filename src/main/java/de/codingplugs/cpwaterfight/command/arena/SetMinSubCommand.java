package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.ArenaCapacityResult;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.AdminSubCommand;
import de.codingplugs.cpwaterfight.display.JoinDisplayManager;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SetMinSubCommand extends AdminSubCommand {

    private final ArenaManager arenaManager;
    private final JoinManager joinManager;
    private final JoinDisplayManager joinDisplayManager;

    public SetMinSubCommand(
            MessageManager messages,
            ArenaManager arenaManager,
            JoinManager joinManager,
            JoinDisplayManager joinDisplayManager
    ) {
        super(messages);
        this.arenaManager = arenaManager;
        this.joinManager = joinManager;
        this.joinDisplayManager = joinDisplayManager;
    }

    @Override
    public String name() {
        return "setmin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(sender, args, 1, messages, "arena.usage.setmin")) {
            return true;
        }

        if (args.length < 3) {
            sendUsage(sender, "arena.usage.setmin");
            return true;
        }

        String id = args[1];
        Optional<Integer> amount = ArenaCommandSupport.parsePositiveInt(args[2]);
        if (amount.isEmpty()) {
            messages.sendPrefixed(sender, "arena.invalid-number");
            return true;
        }

        ArenaCapacityResult result = arenaManager.setMinPlayers(id, amount.get());
        switch (result) {
            case ARENA_NOT_FOUND -> messages.sendPrefixed(sender, "arena.not-found", Map.of("id", id));
            case MIN_GREATER_THAN_MAX -> messages.sendPrefixed(sender, "arena.min-greater-than-max", Map.of(
                    "id", id.toLowerCase(Locale.ROOT),
                    "min", String.valueOf(amount.get())
            ));
            case SUCCESS -> {
                arenaManager.getArena(id).ifPresent(arena -> {
                    ArenaCommandSupport.refreshJoinDisplay(arena, joinManager, joinDisplayManager);
                    messages.sendPrefixed(sender, "arena.min-set", Map.of(
                            "id", arena.id(),
                            "min", String.valueOf(arena.minPlayers())
                    ));
                });
            }
            default -> {
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return ArenaCommandSupport.tabCompleteArenaIds(arenaManager, args[1]);
        }
        return List.of();
    }
}
