package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.BountyEntry;
import fr.crewcmoi.database.BountyTarget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import fr.crewcmoi.utils.MoneyFormat;
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
    private final MalusEffectManager malusEffectManager;

    // Préfixe des équipes de scoreboard utilisées uniquement pour l'affichage visuel
    // (pseudo en rouge + prime en gold), une équipe dédiée par joueur ayant une prime.
    private static final String TEAM_PREFIX = "bounty_";

    public BountyManager(Main plugin, DatabaseManager databaseManager, EconomyManager economyManager,
                          MalusEffectManager malusEffectManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.economyManager = economyManager;
        this.malusEffectManager = malusEffectManager;
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

        addBountyAndRefresh(targetData.getUuid(), targetData.getName(), sender.getUniqueId(), sender.getName(), amount);
        callback.accept(AddResult.SUCCESS);
    }

    /**
     * Ajoute une prime attribuée automatiquement par le serveur (contributeur = null).
     */
    public void addServerBounty(UUID targetUuid, String targetName, double amount) {
        addBountyAndRefresh(targetUuid, targetName, null, "Serveur", amount);
    }

    /**
     * Insère la contribution de prime PUIS recalcule l'affichage/les effets de malus, dans
     * la même tâche asynchrone (donc dans l'ordre garanti). L'ancienne version lançait ces
     * deux étapes comme deux tâches asynchrones séparées : comme le scheduler asynchrone de
     * Bukkit peut les exécuter sur des threads différents sans garantir l'ordre, la lecture
     * du total pouvait s'exécuter AVANT l'écriture de la nouvelle prime, et donc rater le
     * changement (le malus ne s'appliquait alors qu'au prochain recalcul, ex: le kill suivant).
     */
    private void addBountyAndRefresh(UUID targetUuid, String targetName, UUID contributorUuid,
                                      String contributorName, double amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            databaseManager.addBounty(targetUuid, targetName, contributorUuid, contributorName, amount);

            List<BountyEntry> entries = databaseManager.getBounties(targetUuid);
            double total = entries.stream().mapToDouble(BountyEntry::getAmount).sum();
            double serverTotal = entries.stream()
                    .filter(BountyEntry::isServerBounty)
                    .mapToDouble(BountyEntry::getAmount)
                    .sum();

            Bukkit.getScheduler().runTask(plugin, () -> {
                applyBountyDisplay(targetName, total);
                Player player = Bukkit.getPlayer(targetUuid);
                if (player != null && player.isOnline()) {
                    malusEffectManager.updateBountyEffect(player, serverTotal);
                }
            });
        });
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
     * Ne s'applique QUE lors d'une mort causée par un autre joueur (voir BountyListener) :
     * dans ce cas, le killer récupère la TOTALITÉ de la prime de la victime, y compris la
     * part attribuée par le serveur, même s'il en avait lui-même placé une partie. La prime
     * est alors intégralement effacée, ce qui lève au passage l'effet de malchance associé.
     *
     * En cas de mort d'une autre nature (cause naturelle, suicide, etc.), cette méthode
     * n'est jamais appelée : la prime (et donc l'effet de malchance) reste intacte.
     *
     * @return le montant effectivement versé au killer (0 si la victime n'avait aucune prime).
     */
    public double claimBounty(UUID killerUuid, UUID victimUuid) {
        List<BountyEntry> entries = databaseManager.getBounties(victimUuid);
        if (entries.isEmpty()) {
            return 0.0;
        }

        double claimable = entries.stream()
                .mapToDouble(BountyEntry::getAmount)
                .sum();

        databaseManager.clearBounties(victimUuid);

        if (claimable > 0) {
            economyManager.deposit(killerUuid, claimable);
        }

        Player victim = Bukkit.getPlayer(victimUuid);
        if (victim != null) {
            clearBountyDisplay(victim.getName());
            // Toute la prime (y compris la part serveur) vient d'être effacée par ce kill :
            // l'effet de malchance associé est donc levé immédiatement.
            malusEffectManager.updateBountyEffect(victim, 0.0);
        }

        return claimable;
    }

    /**
     * Recalcule (de façon asynchrone) le total des primes actives d'un joueur et met à jour
     * son affichage visuel (pseudo rouge + prime en gold) en conséquence, ainsi que ses
     * effets de malus de prime serveur (réduction de dégâts + coeurs retirés), qui ne
     * dépendent eux que de la part de la prime attribuée par le serveur.
     */
    public void refreshBountyDisplay(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<BountyEntry> entries = databaseManager.getBounties(uuid);
            double total = entries.stream().mapToDouble(BountyEntry::getAmount).sum();
            double serverTotal = entries.stream()
                    .filter(BountyEntry::isServerBounty)
                    .mapToDouble(BountyEntry::getAmount)
                    .sum();
            Bukkit.getScheduler().runTask(plugin, () -> {
                applyBountyDisplay(name, total);
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    malusEffectManager.updateBountyEffect(player, serverTotal);
                }
            });
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
            // Garantit que le suffixe (montant de la prime) s'affiche à la fois dans le tab
            // (liste des joueurs) ET au-dessus de la tête du joueur (nametag) : ce sont les
            // deux endroits gérés par une même équipe de scoreboard sous Bukkit/Spigot.
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");
            team.setSuffix(" §6[§e" + MoneyFormat.format(total) + currency + "§6]");
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
