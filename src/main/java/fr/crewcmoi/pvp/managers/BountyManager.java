package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.database.BountyEntry;
import fr.crewcmoi.pvp.database.BountyTarget;
import fr.crewcmoi.managers.DatabaseManager;
import fr.crewcmoi.economie.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
    private final BountyScoreboardManager displayManager;

    // Cache en mémoire du total de prime actif de chaque joueur, tenu à jour à chaque
    // recalcul (voir applyBountyDisplay/clearBountyDisplay). Permet une lecture instantanée
    // et synchrone, nécessaire pour exposer %crewcmoi_bounty% à PlaceholderAPI (voir
    // BountyPlaceholderExpansion), sans requête base de données à chaque rendu de placeholder
    // (potentiellement plusieurs fois par seconde et par joueur, ex: HUD de RPGhuds).
    private final java.util.Map<UUID, Double> cachedTotals = new java.util.concurrent.ConcurrentHashMap<>();

    public BountyManager(Main plugin, DatabaseManager databaseManager, EconomyManager economyManager,
                          MalusEffectManager malusEffectManager, BountyScoreboardManager displayManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.economyManager = economyManager;
        this.malusEffectManager = malusEffectManager;
        this.displayManager = displayManager;
    }

    public enum AddResult {
        SUCCESS, INVALID_AMOUNT, SELF_TARGET, NOT_ENOUGH_MONEY, TARGET_NOT_FOUND
    }

    /**
     * Ajoute une prime placée par un joueur sur un autre : l'argent est immédiatement débité.
     */
    public void addPlayerBounty(Player sender, String targetName, double amount, String reason, Consumer<AddResult> callback) {
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

        addBountyAndRefresh(targetData.getUuid(), targetData.getName(), sender.getUniqueId(), sender.getName(), amount, reason);
        callback.accept(AddResult.SUCCESS);
    }

    /**
     * Ajoute une prime attribuée automatiquement par le serveur (contributeur = null).
     */
    public void addServerBounty(UUID targetUuid, String targetName, double amount) {
        addBountyAndRefresh(targetUuid, targetName, null, "Serveur", amount, null);
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
                                      String contributorName, double amount, String reason) {
        plugin.getLogger().info("[Bounty] addBountyAndRefresh() appelee pour targetUuid=" + targetUuid
                + " targetName=" + targetName + " amount=" + amount);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                databaseManager.addBounty(targetUuid, targetName, contributorUuid, contributorName, amount, reason);

                List<BountyEntry> entries = databaseManager.getBounties(targetUuid);
                double total = entries.stream().mapToDouble(BountyEntry::getAmount).sum();
                double serverTotal = entries.stream()
                        .filter(BountyEntry::isServerBounty)
                        .mapToDouble(BountyEntry::getAmount)
                        .sum();

                plugin.getLogger().info("[Bounty] Apres insertion : " + entries.size()
                        + " entree(s) en base pour " + targetName + ", total=" + total);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    applyBountyDisplay(targetUuid, targetName, total);
                    Player player = Bukkit.getPlayer(targetUuid);
                    if (player != null && player.isOnline()) {
                        malusEffectManager.updateBountyEffect(player, serverTotal);
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "[Bounty] Erreur dans addBountyAndRefresh pour " + targetName, e);
            }
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

    public void removeDisplayOnQuit(UUID playerUuid) {
        displayManager.remove(playerUuid);
    }

    /**
     * Retourne (de façon synchrone, depuis le cache mémoire) le total de prime actif d'un
     * joueur, ou 0.0 s'il n'en a aucune. Utilisé par BountyPlaceholderExpansion pour exposer
     * %crewcmoi_bounty% à PlaceholderAPI (ex: pour l'intégrer au HUD/nametag de RPGhuds).
     */
    public double getCachedBountyTotal(UUID playerUuid) {
        return cachedTotals.getOrDefault(playerUuid, 0.0);
    }

    /**
     * Résultat d'une réclamation de prime après un kill (voir claimBounty) : le montant
     * effectivement versé au killer, et si cette prime était "légitime" (auquel cas le kill
     * n'est PAS traité comme un kill sans prime valide : pas de malus/mise à prix du killer,
     * voir BountyListener).
     */
    public record BountyClaimResult(double amount, boolean legitimate) {
    }

    /**
     * Tente de faire réclamer par le killer la prime placée sur la victime.
     * Ne s'applique QUE lors d'une mort causée par un autre joueur (voir BountyListener).
     * Le killer touche TOUJOURS l'intégralité de la prime de la victime (y compris les
     * contributions placées par lui-même/ses alliés, ou avec une raison jamais approuvée) :
     * il "récupère la prime qu'avait le joueur sur sa tête" dans tous les cas.
     * Ce qui change, c'est la légitimité retournée, qui détermine si BountyListener applique
     * le malus serveur (mise à prix du killer) :
     *  - légitime (true) si AU MOINS une contribution valide existe : une prime serveur, ou
     *    une prime posée par un joueur qui n'est ni le killer ni un de ses alliés d'équipe
     *    (voir excludedContributor), et qui est soit sans raison, soit approuvée par un admin
     *    (/bounty review) — pas de malus dans ce cas.
     *  - non légitime (false) si la victime n'avait aucune prime, ou si TOUTES ses
     *    contributions viennent du killer/ses alliés, ou ont une raison jamais approuvée :
     *    le kill est alors traité comme un kill sans prime valide (malus + mise à prix du
     *    killer, voir BountyListener), même si de l'argent a quand même été versé au killer.
     * Dans tous les cas, la totalité des contributions de la victime est effacée par ce
     * kill, ce qui lève au passage l'effet de malchance associé.
     *
     * En cas de mort d'une autre nature (cause naturelle, suicide, etc.), cette méthode
     * n'est jamais appelée : la prime (et donc l'effet de malchance) reste intacte.
     *
     * @param excludedContributor prédicat renvoyant vrai pour un contributeur (le killer
     *                             lui-même ou l'un de ses alliés) dont la contribution ne
     *                             doit pas compter comme une prime "légitime".
     */
    public BountyClaimResult claimBounty(UUID killerUuid, UUID victimUuid, Predicate<UUID> excludedContributor) {
        List<BountyEntry> entries = databaseManager.getBounties(victimUuid);
        if (entries.isEmpty()) {
            return new BountyClaimResult(0.0, false);
        }

        double total = 0.0;
        boolean legitimate = false;
        for (BountyEntry entry : entries) {
            total += entry.getAmount();

            boolean excluded = !entry.isServerBounty() && excludedContributor.test(entry.getContributorUuid());
            boolean valid = entry.isServerBounty() || (!excluded && (!entry.hasReason() || entry.isApproved()));
            if (valid) {
                legitimate = true;
            }
        }

        // On efface systématiquement TOUTES les contributions : elles ne peuvent plus être
        // "récupérées" ni révisées plus tard, que la prime ait été jugée légitime ou non.
        databaseManager.clearBounties(victimUuid);

        if (total > 0) {
            economyManager.deposit(killerUuid, total);
        }

        Player victim = Bukkit.getPlayer(victimUuid);
        if (victim != null) {
            clearBountyDisplay(victimUuid);
            // Toute la prime (y compris la part serveur) vient d'être effacée par ce kill :
            // l'effet de malchance associé est donc levé immédiatement.
            malusEffectManager.updateBountyEffect(victim, 0.0);
        }

        return new BountyClaimResult(total, legitimate);
    }

    /**
     * Retourne les contributions de prime avec une raison fournie par un joueur, en attente
     * de validation par un admin (voir /bounty review).
     */
    public List<BountyEntry> getPendingReasonedBounties() {
        return databaseManager.getPendingReasonedBounties();
    }

    /**
     * Approuve une raison de prime : elle devient valide pour un futur kill (le tueur pourra
     * la réclamer, et le kill ne déclenchera pas le malus serveur).
     */
    public void approveBounty(int entryId, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            BountyEntry entry = databaseManager.getBountyEntry(entryId);
            if (entry == null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                return;
            }
            databaseManager.setBountyApproved(entryId, true);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
        });
    }

    /**
     * Refuse une raison de prime : la contribution est supprimée et son montant remboursé
     * au contributeur.
     */
    public void denyBounty(int entryId, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            BountyEntry entry = databaseManager.getBountyEntry(entryId);
            if (entry == null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                return;
            }
            databaseManager.deleteBountyEntry(entryId);
            if (!entry.isServerBounty()) {
                economyManager.deposit(entry.getContributorUuid(), entry.getAmount());
            }

            List<BountyEntry> remaining = databaseManager.getBounties(entry.getTargetUuid());
            double total = remaining.stream().mapToDouble(BountyEntry::getAmount).sum();
            double serverTotal = remaining.stream()
                    .filter(BountyEntry::isServerBounty)
                    .mapToDouble(BountyEntry::getAmount)
                    .sum();

            Bukkit.getScheduler().runTask(plugin, () -> {
                applyBountyDisplay(entry.getTargetUuid(), entry.getTargetName(), total);
                Player target = Bukkit.getPlayer(entry.getTargetUuid());
                if (target != null && target.isOnline()) {
                    malusEffectManager.updateBountyEffect(target, serverTotal);
                }
                callback.accept(true);
            });
        });
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
                applyBountyDisplay(uuid, name, total);
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

    /**
     * Affiche (ou met à jour) la prime du joueur, sous forme de suffixe scoreboard juste
     * sous son pseudo (voir BountyScoreboardManager). N'est affichée que si le
     * joueur a effectivement une prime (montant différent de 0).
     */
    private void applyBountyDisplay(UUID playerUuid, String playerName, double total) {
        try {
            if (total > 0) {
                cachedTotals.put(playerUuid, total);
            } else {
                cachedTotals.remove(playerUuid);
            }
            displayManager.update(playerUuid, playerName, total);
            plugin.getLogger().info("[Bounty] Suffixe de prime mis a jour pour "
                    + playerName + " (montant=" + total + ").");
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "[Bounty] Erreur dans applyBountyDisplay pour " + playerName, e);
        }
    }

    private void clearBountyDisplay(UUID playerUuid) {
        cachedTotals.remove(playerUuid);
        displayManager.remove(playerUuid);
    }
}
