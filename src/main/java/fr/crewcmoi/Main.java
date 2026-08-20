package fr.crewcmoi;

import fr.crewcmoi.commands.AuctionCommand;
import fr.crewcmoi.commands.BalanceCommand;
import fr.crewcmoi.commands.BalanceTopCommand;
import fr.crewcmoi.commands.BountyCommand;
import fr.crewcmoi.commands.ClaimCommand;
import fr.crewcmoi.commands.DuelAcceptCommand;
import fr.crewcmoi.commands.DuelCommand;
import fr.crewcmoi.commands.HomeCommand;
import fr.crewcmoi.commands.InfoCommand;
import fr.crewcmoi.commands.MoneyCommand;
import fr.crewcmoi.commands.PayCommand;
import fr.crewcmoi.commands.SellCommand;
import fr.crewcmoi.commands.SetHomeCommand;
import fr.crewcmoi.commands.SpawnCommand;
import fr.crewcmoi.commands.TeamCommand;
import fr.crewcmoi.commands.TpAcceptCommand;
import fr.crewcmoi.commands.TpaCommand;
import fr.crewcmoi.commands.TpaHereCommand;
import fr.crewcmoi.gui.AuctionGuiManager;
import fr.crewcmoi.gui.BountyGuiManager;
import fr.crewcmoi.gui.BountyReviewGuiManager;
import fr.crewcmoi.gui.ClaimAuctionGuiManager;
import fr.crewcmoi.gui.ClaimSettingsGuiManager;
import fr.crewcmoi.gui.ClaimShopGuiManager;
import fr.crewcmoi.gui.DuelConfigGuiManager;
import fr.crewcmoi.gui.SellGuiManager;
import fr.crewcmoi.listeners.BountyListener;
import fr.crewcmoi.listeners.ClaimListener;
import fr.crewcmoi.listeners.CoinItemListener;
import fr.crewcmoi.listeners.CombatListener;
import fr.crewcmoi.listeners.DuelListener;
import fr.crewcmoi.listeners.GuiListener;
import fr.crewcmoi.listeners.JoinListener;
import fr.crewcmoi.listeners.SellNpcListener;
import fr.crewcmoi.managers.AuctionManager;
import fr.crewcmoi.managers.BountyManager;
import fr.crewcmoi.managers.BountyScoreboardManager;
import fr.crewcmoi.managers.ClaimManager;
import fr.crewcmoi.managers.ClaimVisualizer;
import fr.crewcmoi.managers.CombatManager;
import fr.crewcmoi.managers.DatabaseManager;
import fr.crewcmoi.managers.DuelManager;
import fr.crewcmoi.managers.EconomyManager;
import fr.crewcmoi.managers.HomeManager;
import fr.crewcmoi.managers.MalusEffectManager;
import fr.crewcmoi.managers.PricesManager;
import fr.crewcmoi.managers.SQLiteManager;
import fr.crewcmoi.managers.TeamManager;
import fr.crewcmoi.managers.TeleportManager;
import fr.crewcmoi.vault.VaultEconomyProvider;
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
    private CombatManager combatManager;
    private TeamManager teamManager;
    private BountyManager bountyManager;
    private BountyScoreboardManager bountyScoreboardManager;
    private BountyGuiManager bountyGuiManager;
    private BountyReviewGuiManager bountyReviewGuiManager;
    private MalusEffectManager malusEffectManager;
    private TeleportManager teleportManager;
    private HomeManager homeManager;
    private ClaimManager claimManager;
    private ClaimSettingsGuiManager claimSettingsGuiManager;
    private ClaimShopGuiManager claimShopGuiManager;
    private ClaimVisualizer claimVisualizer;
    private ClaimAuctionGuiManager claimAuctionGuiManager;
    private DuelManager duelManager;
    private DuelConfigGuiManager duelConfigGuiManager;
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
        this.combatManager = new CombatManager(this);
        this.combatManager.startActionBar();
        this.teamManager = new TeamManager(this, databaseManager);
        this.malusEffectManager = new MalusEffectManager(this);
        this.bountyScoreboardManager = new BountyScoreboardManager(this);
        this.bountyScoreboardManager.start();
        this.bountyManager = new BountyManager(this, databaseManager, economyManager, malusEffectManager, bountyScoreboardManager);
        this.bountyGuiManager = new BountyGuiManager(this, bountyManager);
        this.bountyReviewGuiManager = new BountyReviewGuiManager(this, bountyManager);
        this.teleportManager = new TeleportManager(this, combatManager);
        this.homeManager = new HomeManager(this, databaseManager);
        this.claimManager = new ClaimManager(this, databaseManager);
        this.claimManager.loadAll();
        this.claimSettingsGuiManager = new ClaimSettingsGuiManager(this, claimManager);
        this.claimShopGuiManager = new ClaimShopGuiManager(this, claimManager);
        this.claimVisualizer = new ClaimVisualizer(this, claimManager);
        this.claimAuctionGuiManager = new ClaimAuctionGuiManager(this, claimManager);
        this.duelManager = new DuelManager(this, economyManager, combatManager);
        this.duelConfigGuiManager = new DuelConfigGuiManager(this, duelManager);

        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(new JoinListener(this, economyManager, teamManager, bountyManager, malusEffectManager, claimVisualizer), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, sellGuiManager, auctionManager, auctionGuiManager, bountyGuiManager, claimManager, claimSettingsGuiManager, claimShopGuiManager, bountyReviewGuiManager, bountyManager, claimAuctionGuiManager), this);
        getServer().getPluginManager().registerEvents(new BountyListener(this, bountyManager, combatManager, teamManager, economyManager), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this, combatManager, malusEffectManager), this);
        getServer().getPluginManager().registerEvents(new ClaimListener(this, claimManager), this);
        getServer().getPluginManager().registerEvents(new DuelListener(this, duelManager, duelConfigGuiManager), this);
        // Citizens est nécessaire pour déclencher /sell via clic droit sur le NPC d'id 1 :
        // on n'enregistre ce listener (qui référence les classes de son API) que s'il est
        // bien installé et activé, pour éviter un crash au démarrage si absent.
        if (getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getServer().getPluginManager().registerEvents(new SellNpcListener(sellGuiManager), this);
        } else {
            getLogger().warning("Citizens n'est pas détecté : l'ouverture de /sell via clic droit sur le NPC est désactivée.");
        }
        // ItemsAdder est optionnel : on n'enregistre ce listener (qui référence les classes
        // de son API) que s'il est bien installé et activé, pour éviter un crash au démarrage
        // si ce plugin n'est pas présent sur le serveur.
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            getServer().getPluginManager().registerEvents(new CoinItemListener(this, economyManager), this);
        } else {
            getLogger().warning("ItemsAdder n'est pas détecté : la pièce échangeable contre de l'argent est désactivée.");
        }

        // Enregistrement de CrewCMoi comme fournisseur d'économie Vault, pour que les
        // plugins tiers (ex: PlaceholderAPI -> %vault_eco_balance_formatted%, utilisé par
        // le HUD "money" de RPGhuds) puissent lire le solde des joueurs.
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            getServer().getServicesManager().register(
                    net.milkbowl.vault.economy.Economy.class,
                    new VaultEconomyProvider(this, economyManager),
                    this,
                    org.bukkit.plugin.ServicePriority.Highest
            );
            getLogger().info("CrewCMoi enregistré comme fournisseur d'économie Vault.");
        } else {
            getLogger().warning("Vault n'est pas détecté : l'intégration économique Vault (ex: HUD money de RPGhuds) est désactivée.");
        }

        // Enregistrement de l'expansion PlaceholderAPI exposant la prime (%crewcmoi_bounty%,
        // %crewcmoi_bounty_suffix%, %crewcmoi_has_bounty%) : RPGhuds gère l'affichage
        // au-dessus des joueurs par ses propres moyens (packets/HUD, pas le scoreboard
        // vanilla), donc c'est via ce placeholder qu'il faut intégrer la prime dans SA
        // config de nametag pour qu'elle soit réellement visible en jeu. Optionnel comme
        // ItemsAdder ci-dessus : on ne s'enregistre que si PlaceholderAPI est bien présent.
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new fr.crewcmoi.placeholder.BountyPlaceholderExpansion(this, bountyManager).register();
            getLogger().info("Placeholders CrewCMoi enregistrés auprès de PlaceholderAPI "
                    + "(%crewcmoi_bounty%, %crewcmoi_bounty_suffix%, %crewcmoi_has_bounty%).");
        } else {
            getLogger().warning("PlaceholderAPI n'est pas détecté : les placeholders de prime "
                    + "(%crewcmoi_bounty%...) ne sont pas disponibles, RPGhuds ne pourra pas "
                    + "afficher la prime au-dessus des joueurs.");
        }

        // Enregistrement des commandes
        registerCommand("balance", new BalanceCommand(this, economyManager));
        registerCommand("money", new MoneyCommand(this, economyManager));
        registerCommand("baltop", new BalanceTopCommand(this, economyManager));
        registerCommand("sell", new SellCommand(sellGuiManager));
        registerCommand("pay", new PayCommand(this, economyManager));
        registerCommand("ah", new AuctionCommand(this, auctionManager, auctionGuiManager));
        registerCommand("team", new TeamCommand(this, teamManager));
        registerCommand("bounty", new BountyCommand(this, bountyManager, bountyGuiManager, bountyReviewGuiManager));
        registerCommand("info", new InfoCommand(this, bountyManager, malusEffectManager));
        registerCommand("spawn", new SpawnCommand(this));
        registerCommand("tpa", new TpaCommand(this, teleportManager, combatManager));
        registerCommand("tpahere", new TpaHereCommand(this, teleportManager, combatManager));
        registerCommand("tpaccept", new TpAcceptCommand(this, teleportManager));
        registerCommand("sethome", new SetHomeCommand(this, homeManager));
        registerCommand("home", new HomeCommand(this, homeManager));
        ClaimCommand claimCommand = new ClaimCommand(this, claimManager, claimSettingsGuiManager, claimShopGuiManager, claimVisualizer, claimAuctionGuiManager);
        registerCommand("claim", claimCommand);
        registerCommand("claims", claimCommand);
        registerCommand("duel", new DuelCommand(this, duelManager, duelConfigGuiManager, combatManager));
        registerCommand("duelaccept", new DuelAcceptCommand(this, duelManager));

        getLogger().info("EconomyPlugin activé avec succès !");
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.stopActionBar();
        }
        if (bountyScoreboardManager != null) {
            bountyScoreboardManager.stop();
        }
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

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    public BountyReviewGuiManager getBountyReviewGuiManager() {
        return bountyReviewGuiManager;
    }

    public MalusEffectManager getMalusEffectManager() {
        return malusEffectManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public ClaimSettingsGuiManager getClaimSettingsGuiManager() {
        return claimSettingsGuiManager;
    }

    public ClaimShopGuiManager getClaimShopGuiManager() {
        return claimShopGuiManager;
    }

    public ClaimVisualizer getClaimVisualizer() {
        return claimVisualizer;
    }

    public ClaimAuctionGuiManager getClaimAuctionGuiManager() {
        return claimAuctionGuiManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public DuelConfigGuiManager getDuelConfigGuiManager() {
        return duelConfigGuiManager;
    }

}
