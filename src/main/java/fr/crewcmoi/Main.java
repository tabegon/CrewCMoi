package fr.crewcmoi;

import fr.crewcmoi.commands.BalanceCommand;
import fr.crewcmoi.commands.BalanceTopCommand;
import fr.crewcmoi.commands.MoneyCommand;
import fr.crewcmoi.listeners.JoinListener;
import fr.crewcmoi.managers.DatabaseManager;
import fr.crewcmoi.managers.EconomyManager;
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

        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(new JoinListener(this, economyManager), this);

        // Enregistrement des commandes
        registerCommand("balance", new BalanceCommand(this, economyManager));
        registerCommand("money", new MoneyCommand(this, economyManager));
        registerCommand("baltop", new BalanceTopCommand(this, economyManager));

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
}
