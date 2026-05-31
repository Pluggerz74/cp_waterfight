package de.codingplugs.cpwaterfight.command.arena;

import de.codingplugs.cpwaterfight.arena.Arena;
import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.AdminSubCommand;
import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.diagnostics.ArenaDebugReporter;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.level.LevelManager;
import de.codingplugs.cpwaterfight.message.MessageManager;
import de.codingplugs.cpwaterfight.protection.ProtectionSettings;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class DebugArenaSubCommand extends AdminSubCommand {

    private final ArenaManager arenaManager;
    private final JoinManager joinManager;
    private final GameManager gameManager;
    private final LevelManager levelManager;
    private final ConfigManager configManager;
    private final ProtectionSettings protectionSettings;

    public DebugArenaSubCommand(
            MessageManager messages,
            ArenaManager arenaManager,
            JoinManager joinManager,
            GameManager gameManager,
            LevelManager levelManager,
            ConfigManager configManager,
            ProtectionSettings protectionSettings
    ) {
        super(messages);
        this.arenaManager = arenaManager;
        this.joinManager = joinManager;
        this.gameManager = gameManager;
        this.levelManager = levelManager;
        this.configManager = configManager;
        this.protectionSettings = protectionSettings;
    }

    @Override
    public String name() {
        return "debug";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!ArenaCommandSupport.requireArenaArgument(sender, args, 1, messages, "arena.usage.debug")) {
            return true;
        }

        String id = args[1];
        Arena arena = arenaManager.getArena(id).orElse(null);
        if (arena == null) {
            messages.sendPrefixed(sender, "arena.not-found", Map.of("id", id));
            return true;
        }

        messages.sendPrefixed(sender, "debug.header", Map.of(
                "id", arena.id(),
                "display", arena.displayName()
        ));

        Map<String, String> report = ArenaDebugReporter.collect(
                arena,
                joinManager,
                gameManager,
                arenaManager,
                levelManager,
                configManager,
                protectionSettings
        );

        for (Map.Entry<String, String> entry : ArenaDebugReporter.asOrderedLines(report)) {
            messages.send(sender, "debug.line", Map.of(
                    "label", entry.getKey(),
                    "value", entry.getValue()
            ));
        }

        messages.send(sender, "debug.footer", Map.of());
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
