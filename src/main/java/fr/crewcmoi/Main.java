package fr.crewcmoi;

import fr.crewcmoi.claims.commands.ClaimCommand;
import fr.crewcmoi.claims.gui.ClaimAuctionGuiManager;
import fr.crewcmoi.claims.gui.ClaimAdminGuiManager;
import fr.crewcmoi.claims.commands.ClaimAdminCommand;
import fr.crewcmoi.claims.gui.ClaimSettingsGuiManager;
import fr.crewcmoi.claims.gui.ClaimShopGuiManager;
import fr.crewcmoi.claims.listeners.ClaimListener;
import fr.crewcmoi.claims.managers.ClaimManager;
import fr.crewcmoi.claims.managers.ClaimVisualizer;
import fr.crewcmoi.teleport.commands.HomeCommand;
import fr.crewcmoi.moderation.commands.InfoCommand;
import fr.crewcmoi.teleport.commands.SetHomeCommand;
import fr.crewcmoi.teleport.commands.SpawnCommand;
import fr.crewcmoi.pvp.commands.TeamCommand;
import fr.crewcmoi.teleport.commands.TpAcceptCommand;
import fr.crewcmoi.teleport.commands.TpaCommand;
import fr.crewcmoi.teleport.commands.TpaHereCommand;
import fr.crewcmoi.economie.commands.AuctionCommand;
import fr.crewcmoi.economie.commands.BalanceCommand;
import fr.crewcmoi.economie.commands.BalanceTopCommand;
import fr.crewcmoi.economie.commands.MoneyCommand;
import fr.crewcmoi.economie.commands.PayCommand;
import fr.crewcmoi.economie.commands.SellCommand;
import fr.crewcmoi.economie.gui.AuctionGuiManager;
import fr.crewcmoi.economie.gui.SellGuiManager;
import fr.crewcmoi.economie.gui.FishermanGuiManager;
import fr.crewcmoi.economie.listeners.CoinItemListener;
import fr.crewcmoi.economie.listeners.SellNpcListener;
import fr.crewcmoi.economie.listeners.FishermanNpcListener;
import fr.crewcmoi.economie.managers.AuctionManager;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.economie.managers.PricesManager;
import fr.crewcmoi.economie.vault.VaultEconomyProvider;
import fr.crewcmoi.other.listeners.GuiListener;
import fr.crewcmoi.other.listeners.JoinListener;
import fr.crewcmoi.other.managers.DatabaseManager;
import fr.crewcmoi.teleport.managers.HomeManager;
import fr.crewcmoi.other.managers.SQLiteManager;
import fr.crewcmoi.pvp.managers.TeamManager;
import fr.crewcmoi.teleport.managers.TeleportManager;
import fr.crewcmoi.pvp.commands.BountyCommand;
import fr.crewcmoi.pvp.commands.DuelAcceptCommand;
import fr.crewcmoi.pvp.commands.DuelArenaCommand;
import fr.crewcmoi.pvp.commands.DuelCommand;
import fr.crewcmoi.pvp.gui.BountyGuiManager;
import fr.crewcmoi.pvp.gui.BountyReviewGuiManager;
import fr.crewcmoi.pvp.gui.DuelConfigGuiManager;
import fr.crewcmoi.pvp.listeners.BountyListener;
import fr.crewcmoi.pvp.listeners.CombatListener;
import fr.crewcmoi.pvp.listeners.DuelArenaListener;
import fr.crewcmoi.pvp.listeners.DuelListener;
import fr.crewcmoi.pvp.listeners.PlayerHeadDropListener;
import fr.crewcmoi.pvp.listeners.InvisibilityListener;
import fr.crewcmoi.pvp.listeners.PvpRulesListener;
import fr.crewcmoi.pvp.managers.BountyManager;
import fr.crewcmoi.pvp.managers.BountyScoreboardManager;
import fr.crewcmoi.pvp.managers.CombatManager;
import fr.crewcmoi.pvp.managers.DuelArenaManager;
import fr.crewcmoi.pvp.managers.DuelManager;
import fr.crewcmoi.pvp.managers.InvisibilityManager;
import fr.crewcmoi.pvp.managers.MalusEffectManager;
import fr.crewcmoi.moderation.commands.StaffCommand;
import fr.crewcmoi.moderation.commands.VanishCommand;
import fr.crewcmoi.moderation.commands.VanishListCommand;
import fr.crewcmoi.moderation.listeners.StaffModeListener;
import fr.crewcmoi.moderation.listeners.VanishListener;
import fr.crewcmoi.moderation.managers.StaffModeManager;
import fr.crewcmoi.moderation.managers.VanishManager;
import fr.crewcmoi.tab.commands.RoleCommand;
import fr.crewcmoi.tab.listeners.TabListListener;
import fr.crewcmoi.tab.managers.PlayerTeamManager;
import fr.crewcmoi.tab.managers.TabListManager;
import fr.crewcmoi.tab.roles.RoleManager;
import fr.crewcmoi.web.WebDashboardServer;
import fr.crewcmoi.pets.PetManager;
import fr.crewcmoi.pets.PetGuiManager;
import fr.crewcmoi.pets.PetListener;
import fr.crewcmoi.pets.PetsCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

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
    private ClaimAdminGuiManager claimAdminGuiManager;
    private DuelManager duelManager;
    private fr.crewcmoi.pvp.managers.DuelKitManager duelKitManager;
    private DuelConfigGuiManager duelConfigGuiManager;
    private DuelArenaManager duelArenaManager;
    private InvisibilityManager invisibilityManager;
    private RoleManager roleManager;
    private TabListManager tabListManager;
    private PlayerTeamManager playerTeamManager;
    private StaffModeManager staffModeManager;
    private VanishManager vanishManager;
    private FileConfiguration messages;
    private File messagesFile;
    private WebDashboardServer webDashboardServer;
    private PetManager petManager;
    private PetGuiManager petGuiManager;
    private FishermanGuiManager fishermanGuiManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        setupMessages();

        registerCustomMaceRecipe();

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

        this.playerTeamManager = new PlayerTeamManager(this);
        this.playerTeamManager.start();
        this.bountyScoreboardManager = new BountyScoreboardManager(this, playerTeamManager);
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
        this.claimAdminGuiManager = new ClaimAdminGuiManager(this, claimManager, claimSettingsGuiManager);
        this.duelKitManager = new fr.crewcmoi.pvp.managers.DuelKitManager(this);
        this.duelManager = new DuelManager(this, economyManager, combatManager);
        this.duelConfigGuiManager = new DuelConfigGuiManager(this, duelManager);
        this.duelArenaManager = new DuelArenaManager(this);
        this.duelManager.setDuelArenaManager(this.duelArenaManager);
        this.roleManager = new RoleManager(this);
        this.tabListManager = new TabListManager(this, roleManager, playerTeamManager);
        this.vanishManager = new VanishManager(this);
        this.staffModeManager = new StaffModeManager(this, roleManager, tabListManager, vanishManager);
        this.invisibilityManager = new InvisibilityManager(this, playerTeamManager);
        this.petManager = new PetManager(this);
        this.petGuiManager = new PetGuiManager(this, petManager);
        this.fishermanGuiManager = new FishermanGuiManager(this, economyManager);

        getServer().getPluginManager().registerEvents(new JoinListener(this, economyManager, teamManager, bountyManager, malusEffectManager, claimVisualizer, auctionManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, sellGuiManager, auctionManager, auctionGuiManager, bountyGuiManager, claimManager, claimSettingsGuiManager, claimShopGuiManager, bountyReviewGuiManager, bountyManager, claimAuctionGuiManager, claimAdminGuiManager, fishermanGuiManager), this);
        getServer().getPluginManager().registerEvents(new BountyListener(this, bountyManager, combatManager, teamManager, economyManager, invisibilityManager), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this, combatManager, malusEffectManager), this);

        getServer().getPluginManager().registerEvents(new PvpRulesListener(combatManager), this);
        getServer().getPluginManager().registerEvents(new ClaimListener(this, claimManager), this);
        getServer().getPluginManager().registerEvents(new DuelListener(this, duelManager, duelConfigGuiManager), this);
        getServer().getPluginManager().registerEvents(new PlayerHeadDropListener(this, duelManager), this);
        getServer().getPluginManager().registerEvents(new DuelArenaListener(duelArenaManager), this);

        getServer().getPluginManager().registerEvents(new InvisibilityListener(invisibilityManager), this);
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder") && getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            getServer().getPluginManager().registerEvents(new PetListener(this, petManager, petGuiManager), this);
        } else {
            getLogger().warning("Pets désactivés : ItemsAdder et MythicMobs sont nécessaires.");
        }

        getServer().getPluginManager().registerEvents(new TabListListener(tabListManager), this);

        getServer().getPluginManager().registerEvents(new StaffModeListener(staffModeManager, roleManager), this);
        
        getServer().getPluginManager().registerEvents(new VanishListener(vanishManager), this);

        
        if (getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getServer().getPluginManager().registerEvents(new SellNpcListener(sellGuiManager), this);
            getServer().getPluginManager().registerEvents(new FishermanNpcListener(this, fishermanGuiManager), this);
        } else {
            getLogger().warning("Citizens n'est pas détecté : l'ouverture de /sell et du pêcheur via clic droit sur les NPC est désactivée.");
        }

        
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            getServer().getPluginManager().registerEvents(new CoinItemListener(this, economyManager), this);
        } else {
            getLogger().warning("ItemsAdder n'est pas détecté : la pièce échangeable contre de l'argent est désactivée.");
        }

        
        
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

        

        
        
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new fr.crewcmoi.pvp.placeholder.BountyPlaceholderExpansion(this, bountyManager).register();
            getLogger().info("Placeholders CrewCMoi enregistrés auprès de PlaceholderAPI "
                    + "(%crewcmoi_bounty%, %crewcmoi_bounty_suffix%, %crewcmoi_has_bounty%).");
        } else {
            getLogger().warning("PlaceholderAPI n'est pas détecté : les placeholders de prime "
                    + "(%crewcmoi_bounty%...) ne sont pas disponibles, RPGhuds ne pourra pas "
                    + "afficher la prime au-dessus des joueurs.");
        }

        registerCommand("balance", new BalanceCommand(this, economyManager));
        registerCommand("money", new MoneyCommand(this, economyManager));
        registerCommand("baltop", new BalanceTopCommand(this, economyManager));
        registerCommand("sell", new SellCommand(sellGuiManager, pricesManager));
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
        registerCommand("claimadmin", new ClaimAdminCommand(claimAdminGuiManager));
        registerCommand("duel", new DuelCommand(this, duelManager, duelConfigGuiManager, combatManager));
        registerCommand("duelaccept", new DuelAcceptCommand(this, duelManager));
        registerCommand("duelarena", new DuelArenaCommand(duelArenaManager));
        registerCommand("rank", new RoleCommand(this, roleManager, tabListManager));
        registerCommand("staff", new StaffCommand(staffModeManager, roleManager, vanishManager));
        registerCommand("vanish", new VanishCommand(staffModeManager, vanishManager));
        registerCommand("vanishlist", new VanishListCommand(vanishManager));
        registerCommand("pets", new PetsCommand(petGuiManager));

        
        
        this.webDashboardServer = new WebDashboardServer(this, economyManager, pricesManager, claimManager, bountyManager, auctionManager);
        this.webDashboardServer.start();

        getLogger().info("EconomyPlugin activé avec succès !");
    }

    @Override
    public void onDisable() {
        if (petManager != null) {
            petManager.shutdown();
        }
        if (webDashboardServer != null) {
            webDashboardServer.stop();
        }
        if (combatManager != null) {
            combatManager.stopActionBar();
        }
        if (playerTeamManager != null) {
            playerTeamManager.stop();
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
        try (InputStreamReader defConfigStream = new InputStreamReader(getResource("messages.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            messages.setDefaults(defConfig);
        } catch (IOException e) {
            getLogger().warning("Impossible de recharger les messages par défaut : " + e.getMessage());
        }
    }

    

    private void registerCustomMaceRecipe() {
        
        Bukkit.removeRecipe(new NamespacedKey("minecraft", "mace"));

        NamespacedKey key = new NamespacedKey(this, "custom_mace");
        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.MACE));
        recipe.shape(" H ", " D ", " B ");
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('B', Material.BREEZE_ROD);

        Bukkit.addRecipe(recipe);
        getLogger().info("Recette de la Mace personnalisée enregistrée.");
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

    public fr.crewcmoi.pvp.managers.DuelKitManager getDuelKitManager() {
        return duelKitManager;
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

    public DuelArenaManager getDuelArenaManager() {
        return duelArenaManager;
    }

    public DuelConfigGuiManager getDuelConfigGuiManager() {
        return duelConfigGuiManager;
    }

    public InvisibilityManager getInvisibilityManager() {
        return invisibilityManager;
    }

    public PlayerTeamManager getPlayerTeamManager() {
        return playerTeamManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

}
