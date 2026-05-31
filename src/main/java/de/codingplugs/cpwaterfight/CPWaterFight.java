package de.codingplugs.cpwaterfight;

import de.codingplugs.cpwaterfight.arena.ArenaManager;
import de.codingplugs.cpwaterfight.command.HelpSubCommand;
import de.codingplugs.cpwaterfight.command.ReloadSubCommand;
import de.codingplugs.cpwaterfight.command.SubCommand;
import de.codingplugs.cpwaterfight.command.WaterFightCommand;
import de.codingplugs.cpwaterfight.command.arena.AddSpawnSubCommand;
import de.codingplugs.cpwaterfight.command.arena.ArenaInfoSubCommand;
import de.codingplugs.cpwaterfight.command.arena.ArenaListSubCommand;
import de.codingplugs.cpwaterfight.command.arena.CreateArenaSubCommand;
import de.codingplugs.cpwaterfight.command.arena.DeleteArenaSubCommand;
import de.codingplugs.cpwaterfight.command.arena.SetJoinSubCommand;
import de.codingplugs.cpwaterfight.command.arena.SetLobbySubCommand;
import de.codingplugs.cpwaterfight.command.join.JoinArenaSubCommand;
import de.codingplugs.cpwaterfight.command.join.LeaveSubCommand;
import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.display.JoinDisplayManager;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.game.GameStateLabels;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.level.LevelManager;
import de.codingplugs.cpwaterfight.listener.JoinBlockListener;
import de.codingplugs.cpwaterfight.message.MessageManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

/**
 * Entry point for the Water Fight minigame plugin.
 */
public final class CPWaterFight extends JavaPlugin {

    public static final String GAME_MODE_NAME = "Water Fight";

    private ConfigManager configManager;
    private MessageManager messageManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private GameStateLabels gameStateLabels;
    private JoinManager joinManager;
    private JoinDisplayManager joinDisplayManager;
    private LevelManager levelManager;

    private boolean enabled;

    @Override
    public void onEnable() {
        if (!initialize()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        enabled = true;
        getLogger().info(GAME_MODE_NAME + " enabled.");
    }

    @Override
    public void onDisable() {
        if (!enabled) {
            return;
        }

        shutdownManagers();
        enabled = false;
        getLogger().info(GAME_MODE_NAME + " disabled.");
    }

    /**
     * Reloads all configuration and manager state.
     */
    public void reload() {
        joinManager.clear();
        joinDisplayManager.removeAll();

        configManager.reload();
        messageManager.reload();
        arenaManager.reload();
        levelManager.reload();
        gameManager.reload();
        gameStateLabels.reload();

        joinDisplayManager.refreshAll(joinManager::getPlayerCount);
    }

    private boolean initialize() {
        try {
            configManager = new ConfigManager(this);
            if (!configManager.load()) {
                return false;
            }

            messageManager = new MessageManager(configManager);
            messageManager.load();

            arenaManager = new ArenaManager(configManager, getLogger());
            arenaManager.load();

            gameStateLabels = new GameStateLabels(configManager);
            gameStateLabels.load();

            joinDisplayManager = new JoinDisplayManager(
                    configManager,
                    messageManager,
                    arenaManager,
                    null,
                    gameStateLabels
            );

            gameManager = new GameManager(this, configManager, messageManager, joinDisplayManager);
            gameManager.load();

            joinDisplayManager.attachGameManager(gameManager);

            joinManager = new JoinManager(messageManager, arenaManager, joinDisplayManager, gameManager);
            gameManager.setPlayerCountProvider(joinManager::getPlayerCount);
            joinManager.load();
            joinDisplayManager.load();

            levelManager = new LevelManager(configManager);
            levelManager.load();

            registerCommands();
            registerListeners();
            return true;
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to start " + GAME_MODE_NAME, exception);
            return false;
        }
    }

    private void registerCommands() {
        List<SubCommand> subCommands = List.of(
                new HelpSubCommand(messageManager),
                new ReloadSubCommand(this, messageManager),
                new JoinArenaSubCommand(messageManager, arenaManager, joinManager),
                new LeaveSubCommand(messageManager, joinManager),
                new CreateArenaSubCommand(messageManager, arenaManager),
                new DeleteArenaSubCommand(messageManager, arenaManager, joinManager, joinDisplayManager),
                new SetLobbySubCommand(messageManager, arenaManager),
                new SetJoinSubCommand(messageManager, arenaManager, joinManager, joinDisplayManager),
                new AddSpawnSubCommand(messageManager, arenaManager),
                new ArenaInfoSubCommand(messageManager, arenaManager),
                new ArenaListSubCommand(messageManager, arenaManager)
        );

        WaterFightCommand executor = new WaterFightCommand(messageManager, subCommands);
        PluginCommand command = getCommand("waterfight");
        if (command == null) {
            throw new IllegalStateException("Command 'waterfight' is not defined in plugin.yml.");
        }

        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new JoinBlockListener(arenaManager, joinManager, messageManager),
                this
        );
    }

    private void shutdownManagers() {
        if (joinDisplayManager != null) {
            joinDisplayManager.shutdown();
        }
        if (joinManager != null) {
            joinManager.shutdown();
        }
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (levelManager != null) {
            levelManager.shutdown();
        }
        if (arenaManager != null) {
            arenaManager.shutdown();
        }
        if (configManager != null) {
            configManager.shutdown();
        }
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public MessageManager messageManager() {
        return messageManager;
    }

    public ArenaManager arenaManager() {
        return arenaManager;
    }

    public GameManager gameManager() {
        return gameManager;
    }

    public JoinManager joinManager() {
        return joinManager;
    }

    public JoinDisplayManager joinDisplayManager() {
        return joinDisplayManager;
    }

    public LevelManager levelManager() {
        return levelManager;
    }

    public boolean isPluginReady() {
        return enabled;
    }
}
