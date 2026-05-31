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
import de.codingplugs.cpwaterfight.command.arena.ForceStartSubCommand;
import de.codingplugs.cpwaterfight.command.arena.SetJoinSubCommand;
import de.codingplugs.cpwaterfight.command.arena.SetLobbySubCommand;
import de.codingplugs.cpwaterfight.command.arena.RenameArenaSubCommand;
import de.codingplugs.cpwaterfight.command.arena.SetMaxSubCommand;
import de.codingplugs.cpwaterfight.command.arena.SetMinSubCommand;
import de.codingplugs.cpwaterfight.command.arena.StopGameSubCommand;
import de.codingplugs.cpwaterfight.command.arena.TpArenaSubCommand;
import de.codingplugs.cpwaterfight.command.arena.ValidateArenaSubCommand;
import de.codingplugs.cpwaterfight.command.join.JoinArenaSubCommand;
import de.codingplugs.cpwaterfight.command.join.LeaveSubCommand;
import de.codingplugs.cpwaterfight.command.VersionSubCommand;
import de.codingplugs.cpwaterfight.command.arena.DebugArenaSubCommand;
import de.codingplugs.cpwaterfight.config.ConfigManager;
import de.codingplugs.cpwaterfight.config.ConfigSanityChecker;
import de.codingplugs.cpwaterfight.display.JoinDisplayManager;
import de.codingplugs.cpwaterfight.feedback.FeedbackManager;
import de.codingplugs.cpwaterfight.game.GameManager;
import de.codingplugs.cpwaterfight.game.GameStateLabels;
import de.codingplugs.cpwaterfight.join.JoinManager;
import de.codingplugs.cpwaterfight.level.LevelManager;
import de.codingplugs.cpwaterfight.listener.GameLifecycleListener;
import de.codingplugs.cpwaterfight.listener.GameProtectionListener;
import de.codingplugs.cpwaterfight.listener.JoinBlockListener;
import de.codingplugs.cpwaterfight.message.MessageManager;
import de.codingplugs.cpwaterfight.protection.ProtectionSettings;
import de.codingplugs.cpwaterfight.scoreboard.ScoreboardManager;
import de.codingplugs.cpwaterfight.spectator.SpectatorManager;
import de.codingplugs.cpwaterfight.spectator.SpectatorSettings;
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
    private ScoreboardManager scoreboardManager;
    private ProtectionSettings protectionSettings;
    private SpectatorSettings spectatorSettings;
    private SpectatorManager spectatorManager;
    private FeedbackManager feedbackManager;

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
        if (spectatorManager != null) {
            spectatorManager.restoreAll();
        }

        joinManager.clear();
        joinDisplayManager.removeAll();
        if (scoreboardManager != null) {
            scoreboardManager.shutdown();
        }
        if (feedbackManager != null) {
            feedbackManager.shutdown();
        }

        configManager.reload();
        messageManager.reload();
        arenaManager.reload();
        levelManager.reload();
        gameManager.reload();
        gameStateLabels.reload();
        if (scoreboardManager != null) {
            scoreboardManager.reload();
        }
        if (feedbackManager != null) {
            feedbackManager.reload();
        }

        joinDisplayManager.refreshAll(joinManager::getPlayerCount);
        runConfigSanityCheck();
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

            levelManager = new LevelManager(configManager, getLogger());
            levelManager.load();

            gameManager = new GameManager(
                    this,
                    configManager,
                    messageManager,
                    joinDisplayManager,
                    levelManager
            );
            gameManager.load();

            scoreboardManager = new ScoreboardManager(this, configManager, gameManager, levelManager);
            protectionSettings = new ProtectionSettings(configManager);
            spectatorSettings = new SpectatorSettings(configManager);

            joinDisplayManager.attachGameManager(gameManager);

            joinManager = new JoinManager(
                    messageManager,
                    arenaManager,
                    joinDisplayManager,
                    gameManager,
                    scoreboardManager
            );
            gameManager.setPlayerCountProvider(joinManager::getPlayerCount);
            gameManager.setJoinManager(joinManager);
            gameManager.setScoreboardManager(scoreboardManager);

            spectatorManager = new SpectatorManager(
                    this,
                    spectatorSettings,
                    gameManager,
                    joinManager,
                    messageManager
            );
            joinManager.setSpectatorManager(spectatorManager);
            gameManager.setSpectatorManager(spectatorManager);

            feedbackManager = new FeedbackManager(
                    this,
                    configManager,
                    messageManager,
                    gameManager,
                    joinManager,
                    levelManager
            );
            joinManager.setFeedbackManager(feedbackManager);
            gameManager.setFeedbackManager(feedbackManager);
            feedbackManager.load();

            scoreboardManager.setJoinManager(joinManager);
            joinManager.load();
            joinDisplayManager.load();
            scoreboardManager.load();

            registerCommands();
            registerListeners();
            runConfigSanityCheck();
            return true;
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to start " + GAME_MODE_NAME, exception);
            return false;
        }
    }

    private void registerCommands() {
        List<SubCommand> subCommands = List.of(
                new HelpSubCommand(messageManager),
                new VersionSubCommand(this, messageManager),
                new ReloadSubCommand(this, messageManager),
                new JoinArenaSubCommand(messageManager, arenaManager, joinManager),
                new LeaveSubCommand(messageManager, joinManager),
                new CreateArenaSubCommand(messageManager, arenaManager),
                new DeleteArenaSubCommand(messageManager, arenaManager, joinManager, joinDisplayManager),
                new SetLobbySubCommand(messageManager, arenaManager),
                new SetJoinSubCommand(messageManager, arenaManager, joinManager, joinDisplayManager),
                new AddSpawnSubCommand(messageManager, arenaManager),
                new ArenaInfoSubCommand(messageManager, arenaManager),
                new ArenaListSubCommand(messageManager, arenaManager),
                new ForceStartSubCommand(messageManager, arenaManager, gameManager),
                new StopGameSubCommand(messageManager, arenaManager, gameManager),
                new TpArenaSubCommand(messageManager, arenaManager),
                new SetMinSubCommand(messageManager, arenaManager, joinManager, joinDisplayManager),
                new SetMaxSubCommand(messageManager, arenaManager, joinManager, joinDisplayManager),
                new RenameArenaSubCommand(messageManager, arenaManager, joinManager, joinDisplayManager),
                new ValidateArenaSubCommand(messageManager, arenaManager),
                new DebugArenaSubCommand(
                        messageManager,
                        arenaManager,
                        joinManager,
                        gameManager,
                        levelManager,
                        configManager,
                        protectionSettings,
                        spectatorManager
                )
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
        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(
                new JoinBlockListener(arenaManager, joinManager, messageManager),
                this
        );
        pluginManager.registerEvents(new GameLifecycleListener(gameManager, spectatorManager), this);
        pluginManager.registerEvents(
                new GameProtectionListener(joinManager, gameManager, messageManager, protectionSettings),
                this
        );
    }

    private void shutdownManagers() {
        if (joinDisplayManager != null) {
            joinDisplayManager.shutdown();
        }
        if (scoreboardManager != null) {
            scoreboardManager.shutdown();
        }
        if (joinManager != null) {
            joinManager.shutdown();
        }
        if (spectatorManager != null) {
            spectatorManager.shutdown();
        }
        if (feedbackManager != null) {
            feedbackManager.shutdown();
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

    private void runConfigSanityCheck() {
        if (configManager == null || levelManager == null || protectionSettings == null) {
            return;
        }

        new ConfigSanityChecker(configManager, levelManager, protectionSettings).validate(getLogger());
    }
}
