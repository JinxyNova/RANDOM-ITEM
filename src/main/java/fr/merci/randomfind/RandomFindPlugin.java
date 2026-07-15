package fr.merci.randomfind;

import org.bukkit.plugin.java.JavaPlugin;

public class RandomFindPlugin extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.gameManager = new GameManager(this);

        RFCommand command = new RFCommand(this, gameManager);
        getCommand("rf").setExecutor(command);
        getCommand("rf").setTabCompleter(command);

        getLogger().info("RandomFind active !");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.stopGame(false);
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
