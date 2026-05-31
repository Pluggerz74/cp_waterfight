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
import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.level.LevelManager;
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
    private JoinManager joinManager;
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
        configManager.reload();
        messageManager.reload();
        arenaManager.reload();
        levelManager.reload();
        gameManager.reload();
        joinManager.reload();
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

            gameManager = new GameManager();
            gameManager.load();

            joinManager = new JoinManager();
            joinManager.load();

            levelManager = new LevelManager(configManager);
            levelManager.load();

            registerCommands();
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
                new CreateArenaSubCommand(messageManager, arenaManager),
                new DeleteArenaSubCommand(messageManager, arenaManager),
                new SetLobbySubCommand(messageManager, arenaManager),
                new SetJoinSubCommand(messageManager, arenaManager),
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

    private void shutdownManagers() {
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

    public LevelManager levelManager() {
        return levelManager;
    }

    public boolean isPluginReady() {
        return enabled;
    }
}
