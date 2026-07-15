package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.BountyEntry;
import fr.crewcmoi.database.BountyTarget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.text.DecimalFormat;
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
    private final DecimalFormat format = new DecimalFormat("#,##0.00");

    // Préfixe des équipes de scoreboard utilisées uniquement pour l'affichage visuel
    // (pseudo en rouge + prime en gold), une équipe dédiée par joueur ayant une prime.
    private static final String TEAM_PREFIX = "bounty_";

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

        refreshBountyDisplay(targetUuid, targetDisplayName);
        callback.accept(AddResult.SUCCESS);
    }

    /**
     * Ajoute une prime attribuée automatiquement par le serveur (contributeur = null).
     */
    public void addServerBounty(UUID targetUuid, String targetName, double amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                databaseManager.addBounty(targetUuid, targetName, null, "Serveur", amount));
        refreshBountyDisplay(targetUuid, targetName);
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

        Player victim = Bukkit.getPlayer(victimUuid);
        if (victim != null) {
            clearBountyDisplay(victim.getName());
        }

        return claimable;
    }

    /**
     * Recalcule (de façon asynchrone) le total des primes actives d'un joueur et met à jour
     * son affichage visuel (pseudo rouge + prime en gold) en conséquence.
     */
    public void refreshBountyDisplay(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<BountyEntry> entries = databaseManager.getBounties(uuid);
            double total = entries.stream().mapToDouble(BountyEntry::getAmount).sum();
            Bukkit.getScheduler().runTask(plugin, () -> applyBountyDisplay(name, total));
        });
    }

    /**
     * Réapplique l'affichage de prime d'un joueur qui vient de se connecter (synchrone, à
     * appeler depuis le thread principal au join).
     */
    public void refreshBountyDisplayOnJoin(UUID uuid, String name) {
        refreshBountyDisplay(uuid, name);
    }

    private void applyBountyDisplay(String playerName, double total) {
        if (total > 0) {
            Scoreboard board = getMainScoreboard();
            String teamName = teamName(playerName);
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            team.setColor(ChatColor.RED);
            String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");
            team.setSuffix(" §6[§e" + format.format(total) + currency + "§6]");
            if (!team.hasEntry(playerName)) {
                team.addEntry(playerName);
            }
        } else {
            clearBountyDisplay(playerName);
        }
    }

    private void clearBountyDisplay(String playerName) {
        Scoreboard board = getMainScoreboard();
        Team team = board.getTeam(teamName(playerName));
        if (team != null) {
            team.unregister();
        }
    }

    private String teamName(String playerName) {
        // Les noms d'équipe sont limités en longueur sur certaines versions : on tronque prudemment.
        String base = TEAM_PREFIX + playerName;
        return base.length() > 40 ? base.substring(0, 40) : base;
    }

    private Scoreboard getMainScoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }
}
