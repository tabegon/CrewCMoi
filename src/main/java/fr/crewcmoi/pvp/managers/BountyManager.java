package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.database.BountyEntry;
import fr.crewcmoi.pvp.database.BountyTarget;
import fr.crewcmoi.other.managers.DatabaseManager;
import fr.crewcmoi.economie.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BountyManager {

    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    private final MalusEffectManager malusEffectManager;
    private final BountyScoreboardManager displayManager;

    

    
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

    public void addServerBounty(UUID targetUuid, String targetName, double amount) {
        addBountyAndRefresh(targetUuid, targetName, null, "Serveur", amount, null);
    }

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

    public double getCachedBountyTotal(UUID playerUuid) {
        return cachedTotals.getOrDefault(playerUuid, 0.0);
    }

    public record BountyClaimResult(double amount, boolean legitimate) {
    }

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

        
        databaseManager.clearBounties(victimUuid);

        if (total > 0) {
            economyManager.deposit(killerUuid, total);
        }

        Player victim = Bukkit.getPlayer(victimUuid);
        if (victim != null) {
            clearBountyDisplay(victimUuid);

            malusEffectManager.updateBountyEffect(victim, 0.0);
        }

        return new BountyClaimResult(total, legitimate);
    }

    public List<BountyEntry> getPendingReasonedBounties() {
        return databaseManager.getPendingReasonedBounties();
    }

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

    public void refreshBountyDisplayOnJoin(UUID uuid, String name) {
        refreshBountyDisplay(uuid, name);
    }

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
