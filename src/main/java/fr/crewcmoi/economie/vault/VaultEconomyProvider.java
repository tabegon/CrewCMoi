package fr.crewcmoi.economie.vault;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.managers.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fr.crewcmoi.utils.MoneyFormat;

/**
 * Implémentation de l'interface Vault Economy, qui délègue toutes les opérations
 * au système économique interne de CrewCMoi (EconomyManager / base SQLite).
 *
 * Permet à n'importe quel plugin utilisant l'API Vault (dont PlaceholderAPI via son
 * extension "Vault", utilisée notamment par RPGhuds pour son HUD "money") de lire et
 * modifier le solde des joueurs via le système économique de CrewCMoi, sans qu'il ait
 * besoin de connaître son fonctionnement interne.
 *
 * Ce plugin ne gère volontairement pas les banques (hasBankSupport() renvoie false) :
 * seule l'économie individuelle des joueurs est prise en charge.
 *
 * NOTE : cette classe a été écrite d'après la signature standard et stable de
 * l'interface net.milkbowl.vault.economy.Economy (API Vault). Comme aucune dépendance
 * Vault n'était présente dans le projet fourni, assurez-vous d'ajouter le jar/dépendance
 * Vault (ex: via jitpack.io "com.github.MilkBowl:VaultAPI:1.7") à votre outil de build
 * (Maven/Gradle) avant de compiler, sans quoi cette classe ne compilera pas.
 */
public class VaultEconomyProvider implements Economy {

    private final Main plugin;
    private final EconomyManager economyManager;

    public VaultEconomyProvider(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "CrewCMoi";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        String currency = plugin.getConfig().getString("economy.currency-symbol", "");
        return MoneyFormat.format(amount) + currency;
    }

    @Override
    public String currencyNamePlural() {
        return plugin.getConfig().getString("economy.currency-symbol", "");
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getConfig().getString("economy.currency-symbol", "");
    }

    @Override
    public boolean hasAccount(String playerName) {
        return economyManager.getPlayerDataByName(playerName) != null;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return economyManager.getPlayerData(player.getUniqueId()) != null;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        UUID uuid = resolveUuid(playerName);
        return uuid != null ? economyManager.getBalance(uuid) : 0.0;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return economyManager.getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        UUID uuid = resolveUuid(playerName);
        return uuid != null && economyManager.has(uuid, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return economyManager.has(player.getUniqueId(), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        UUID uuid = resolveUuid(playerName);
        if (uuid == null) {
            return failure(amount, "Joueur inconnu de CrewCMoi.");
        }
        return withdrawPlayer(uuid, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return withdrawPlayer(player.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        UUID uuid = resolveUuid(playerName);
        if (uuid == null) {
            return failure(amount, "Joueur inconnu de CrewCMoi.");
        }
        return depositPlayer(uuid, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return depositPlayer(player.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    private EconomyResponse withdrawPlayer(UUID uuid, double amount) {
        boolean success = economyManager.withdraw(uuid, amount);
        double balance = economyManager.getBalance(uuid);
        if (success) {
            return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, "");
        }
        return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Solde insuffisant.");
    }

    private EconomyResponse depositPlayer(UUID uuid, double amount) {
        economyManager.deposit(uuid, amount);
        double balance = economyManager.getBalance(uuid);
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    private EconomyResponse failure(double amount, String message) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, message);
    }

    private UUID resolveUuid(String playerName) {
        var data = economyManager.getPlayerDataByName(playerName);
        if (data != null) {
            return data.getUuid();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }

    // ===================== Banques : non supportées =====================

    @Override
    public EconomyResponse createBank(String name, String player) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return failure(0, "Les banques ne sont pas supportées par CrewCMoi.");
    }

    @Override
    public List<String> getBanks() {
        return new ArrayList<>();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return false;
    }
}
