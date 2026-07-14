package fr.crewcmoi;

import fr.crewcmoi.commands.AuctionCommand;
import fr.crewcmoi.commands.BalanceCommand;
import fr.crewcmoi.commands.BalanceTopCommand;
import fr.crewcmoi.commands.MoneyCommand;
import fr.crewcmoi.commands.PayCommand;
import fr.crewcmoi.commands.SellCommand;
import fr.crewcmoi.gui.AuctionGuiManager;
import fr.crewcmoi.gui.SellGuiManager;
import fr.crewcmoi.listeners.GuiListener;
import fr.crewcmoi.listeners.JoinListener;
import fr.crewcmoi.managers.AuctionManager;
import fr.crewcmoi.managers.DatabaseManager;
import fr.crewcmoi.managers.EconomyManager;
import fr.crewcmoi.managers.PricesManager;
import fr.crewcmoi.managers.SQLiteManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Main extends JavaPlugin {

    private static Main instance;

    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private PricesManager pricesManager;
    private SellGuiManager sellGuiManager;
    private AuctionManager auctionManager;
    private AuctionGuiManager auctionGuiManager;
    private FileConfiguration messages;
    private File messagesFile;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        setupMessages();

        // Initialisation de la base de données (SQLite par défaut)
        this.databaseManager = new SQLiteManager(this);
        this.databaseManager.connect();
        this.databaseManager.init();

        this.economyManager = new EconomyManager(this, databaseManager);
        this.pricesManager = new PricesManager(this);
        this.sellGuiManager = new SellGuiManager(this, pricesManager, economyManager);
        this.auctionManager = new AuctionManager(this, databaseManager, economyManager);
        this.auctionGuiManager = new AuctionGuiManager(this, auctionManager);

        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(new JoinListener(this, economyManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, sellGuiManager, auctionManager, auctionGuiManager), this);

        // Enregistrement des commandes
        registerCommand("balance", new BalanceCommand(this, economyManager));
        registerCommand("money", new MoneyCommand(this, economyManager));
        registerCommand("baltop", new BalanceTopCommand(this, economyManager));
        registerCommand("sell", new SellCommand(sellGuiManager));
        registerCommand("pay", new PayCommand(this, economyManager));
        registerCommand("ah", new AuctionCommand(this, auctionManager, auctionGuiManager));

        getLogger().info("EconomyPlugin activé avec succès !");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("EconomyPlugin désactivé.");
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter) {
                getCommand(name).setTabCompleter((org.bukkit.command.TabCompleter) executor);
            }
        } else {
            getLogger().warning("Impossible d'enregistrer la commande /" + name + " (absente du plugin.yml)");
        }
    }

    private void setupMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Charge les valeurs par défaut depuis le jar au cas où certaines clés manquent
        try (InputStreamReader defConfigStream = new InputStreamReader(
                getResource("messages.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            messages.setDefaults(defConfig);
        } catch (IOException e) {
            getLogger().warning("Impossible de charger les messages par défaut : " + e.getMessage());
        }
    }

    public void reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public static Main getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public PricesManager getPricesManager() {
        return pricesManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }
}
