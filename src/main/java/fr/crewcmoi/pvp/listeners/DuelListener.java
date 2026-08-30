package fr.crewcmoi.pvp.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.gui.DuelConfigGuiManager;
import fr.crewcmoi.pvp.gui.DuelConfigHolder;
import fr.crewcmoi.pvp.managers.DuelManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Gère :
 *  - les clics dans la GUI /duel (configuration des règles puis envoi de la demande) ;
 *  - la résolution d'un duel à la mort d'un des deux participants (application du
 *    keepinventory, versement de la mise au gagnant) ;
 *  - le forfait automatique en cas de déconnexion pendant un duel (ou l'annulation
 *    d'une demande en attente).
 */
public class DuelListener implements Listener {

    private final Main plugin;
    private final DuelManager duelManager;
    private final DuelConfigGuiManager duelConfigGuiManager;

    public DuelListener(Main plugin, DuelManager duelManager, DuelConfigGuiManager duelConfigGuiManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
        this.duelConfigGuiManager = duelConfigGuiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder rawHolder = event.getInventory().getHolder();
        if (!(rawHolder instanceof DuelConfigHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        Inventory gui = event.getInventory();
        if (slot < 0 || slot >= gui.getSize()) {
            return;
        }

        if (slot == DuelConfigHolder.KEEPINVENTORY_SLOT) {
            holder.setKeepInventory(!holder.isKeepInventory());
            duelConfigGuiManager.render(holder);
        } else if (slot == DuelConfigHolder.BET_SLOT) {
            double step = duelManager.getBetStep();
            double bet = holder.getBet();
            if (event.getClick().isLeftClick()) {
                bet += step;
            } else if (event.getClick().isRightClick()) {
                bet = Math.max(0, bet - step);
            } else {
                bet = 0;
            }
            double maxBet = duelManager.getMaxBet();
            if (maxBet > 0 && bet > maxBet) {
                bet = maxBet;
            }
            holder.setBet(bet);
            duelConfigGuiManager.render(holder);
        } else if (slot == DuelConfigHolder.CANCEL_SLOT) {
            player.closeInventory();
        } else if (slot == DuelConfigHolder.CONFIRM_SLOT) {
            player.closeInventory();

            org.bukkit.entity.Player target = plugin.getServer().getPlayer(holder.getTargetUuid());
            if (target == null || !target.isOnline()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix", "") +
                                plugin.getMessages().getString("general.player-not-found",
                                        "&cCe joueur n'existe pas ou n'est pas en ligne.")));
                return;
            }
            if (duelManager.isInDuel(player.getUniqueId()) || duelManager.isInDuel(target.getUniqueId())) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix", "") +
                                plugin.getMessages().getString("duel.already-in-duel", "&cCe joueur est déjà en duel.")));
                return;
            }

            duelManager.createRequest(player, target, holder.isKeepInventory(), holder.getBet());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        var session = duelManager.getSession(victim.getUniqueId());
        if (session == null) {
            return;
        }

        if (session.isKeepInventory()) {
            event.getDrops().clear();
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }

        duelManager.handleDeath(victim);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        duelManager.handleQuit(event.getPlayer());
    }
}
