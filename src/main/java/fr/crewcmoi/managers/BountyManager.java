package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.BountyEntry;
import fr.crewcmoi.database.BountyTarget;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Gère la logique des primes (bounty) :
 * - ajout d'une prime par un joueur sur un autre (/bounty add)
 * - réclamation de la prime en tuant la cible
 * - attribution automatique d'une prime + malus par le serveur en cas d'agression injustifiée
 */
public class BountyManager {

    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;

    public BountyManager(Main plugin, DatabaseManager databaseManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.economyManager = economyManager;
    }

    public enum AddResult {
        SUCCESS, INVALID_AMOUNT, SELF_TARGET, NOT_ENOUGH_MONEY, TARGET_NOT_FOUND
    }

    /**
     * Ajoute une prime placée par un joueur sur un autre : l'argent est immédiatement débité.
     */
    public void addPlayerBounty(Player sender, String targetName, double amount, Consumer<AddResult> callback) {
        if (!Double.isFinite(amount) || amount <= 0) {
            callback.accept(AddResult.INVALID_AMOUNT);
            return;
        }

        var targetData = economyManager.getPlayerDataByName(targetName);
        if (targetData == null) {
            callback.accept(AddResult.TARGET_NOT_FOUND);
            return;
        }

        if (targetData.getUuid().equals(sender.getUniqueId())) {
            callback.accept(AddResult.SELF_TARGET);
            return;
        }

        if (!economyManager.withdraw(sender.getUniqueId(), amount)) {
            callback.accept(AddResult.NOT_ENOUGH_MONEY);
            return;
        }

        UUID targetUuid = targetData.getUuid();
        String targetDisplayName = targetData.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                databaseManager.addBounty(targetUuid, targetDisplayName, sender.getUniqueId(), sender.getName(), amount));

        callback.accept(AddResult.SUCCESS);
    }

    /**
     * Ajoute une prime attribuée automatiquement par le serveur (contributeur = null).
     */
    public void addServerBounty(UUID targetUuid, String targetName, double amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                databaseManager.addBounty(targetUuid, targetName, null, "Serveur", amount));
    }

    public int getServerBountyCountToday(UUID playerUuid) {
        return databaseManager.getServerBountyCountToday(playerUuid);
    }

    public void incrementServerBountyCount(UUID playerUuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> databaseManager.incrementServerBountyCount(playerUuid));
    }

    public List<BountyEntry> getBounties(UUID targetUuid) {
        return databaseManager.getBounties(targetUuid);
    }

    public List<BountyTarget> getBountyTargets() {
        return databaseManager.getBountyTargets();
    }

    /**
     * Tente de faire réclamer par le killer la prime placée sur la victime.
     * Seules les contributions venant d'un joueur différent du killer sont payées
     * (une prime placée par le killer lui-même sur sa propre victime ne peut pas être récupérée).
     * Dans tous les cas, si la victime avait une prime active, elle est réinitialisée après sa mort.
     *
     * @return le montant effectivement versé au killer (0 si aucune prime réclamable).
     */
    public double claimBounty(UUID killerUuid, UUID victimUuid) {
        List<BountyEntry> entries = databaseManager.getBounties(victimUuid);
        if (entries.isEmpty()) {
            return 0.0;
        }

        double claimable = entries.stream()
                .filter(e -> e.getContributorUuid() == null || !e.getContributorUuid().equals(killerUuid))
                .mapToDouble(BountyEntry::getAmount)
                .sum();

        databaseManager.clearBounties(victimUuid);

        if (claimable > 0) {
            economyManager.deposit(killerUuid, claimable);
        }

        return claimable;
    }
}
