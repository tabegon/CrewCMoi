package fr.crewcmoi.pvp.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.gui.DuelConfigGuiManager;
import fr.crewcmoi.pvp.gui.DuelConfigHolder;
import fr.crewcmoi.pvp.managers.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère :
 *  - les clics dans la GUI /duel (configuration des règles puis envoi de la demande) ;
 *  - la saisie au clavier (dans le chat) du montant de la mise, déclenchée par un
 *    clic molette sur l'item de mise ;
 *  - la résolution d'un duel à la mort d'un des deux participants (application du
 *    keepinventory, versement de la mise au gagnant) ;
 *  - le forfait automatique en cas de déconnexion pendant un duel (ou l'annulation
 *    d'une demande en attente).
 */
public class DuelListener implements Listener {

    private final Main plugin;
    private final DuelManager duelManager;
    private final DuelConfigGuiManager duelConfigGuiManager;

    // Joueurs en train de taper leur mise dans le chat suite à un clic molette.
    private final Map<UUID, DuelConfigHolder> awaitingBetInput = new ConcurrentHashMap<>();

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
        } else if (slot == DuelConfigHolder.KIT_SLOT) {
            if (holder.getKitId() == null) {
                holder.setKitId("basic");
            } else {
                holder.setKitId(null);
            }
            duelConfigGuiManager.render(holder);
        } else if (slot == DuelConfigHolder.DROPHEAD_SLOT) {
            holder.setDropHead(!holder.isDropHead());
            duelConfigGuiManager.render(holder);
        } else if (slot == DuelConfigHolder.BET_SLOT) {
            if (event.getClick() == ClickType.MIDDLE) {
                // Clic molette : on demande le montant directement dans le chat plutôt
                // que de l'ajuster pas à pas.
                awaitingBetInput.put(player.getUniqueId(), holder);
                player.closeInventory();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6&lDuel &8» &eTapez le montant exact de la mise dans le chat &7(ou &c'annuler'&7 pour revenir)."));
                return;
            }

            double step = duelManager.getBetStep();
            double bet = holder.getBet();
            if (event.getClick().isLeftClick()) {
                bet += step;
            } else if (event.getClick().isRightClick()) {
                bet = Math.max(0, bet - step);
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

            duelManager.createRequest(player, target, holder.isKeepInventory(), holder.getBet(), holder.isDropHead(), holder.getKitId());
        }
    }

    /**
     * Capture la saisie du chat pour les joueurs qui viennent de cliquer-molette sur
     * l'item de mise, afin de leur permettre de rentrer un montant exact.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBetChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        DuelConfigHolder holder = awaitingBetInput.remove(player.getUniqueId());
        if (holder == null) {
            return;
        }

        event.setCancelled(true);
        String input = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (input.equalsIgnoreCase("annuler") || input.equalsIgnoreCase("cancel")) {
                reopenBetGui(player, holder);
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(input.replace(",", ".").trim());
            } catch (NumberFormatException exception) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6&lDuel &8» &cMontant invalide, la mise n'a pas été changée."));
                reopenBetGui(player, holder);
                return;
            }

            if (amount < 0) {
                amount = 0;
            }
            double maxBet = duelManager.getMaxBet();
            if (maxBet > 0 && amount > maxBet) {
                amount = maxBet;
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6&lDuel &8» &eLa mise a été limitée au maximum autorisé."));
            }

            holder.setBet(amount);
            reopenBetGui(player, holder);
        });
    }

    private void reopenBetGui(Player player, DuelConfigHolder holder) {
        duelConfigGuiManager.render(holder);
        player.openInventory(holder.getInventory());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        var session = duelManager.getSession(victim.getUniqueId());
        if (session == null) {
            return;
        }

        if (session.isKeepInventory() || session.hasKit()) {
            event.getDrops().clear();
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }

        if (session.isDropHead()) {
            double price = plugin.getConfig().getDouble("player-head-drop.price", 250.0);
            ItemStack head = PlayerHeadDropListener.createHead(plugin, victim, price);
            if (head != null) {
                victim.getWorld().dropItemNaturally(victim.getLocation(), head);
            }
        }

        duelManager.handleDeath(victim);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        awaitingBetInput.remove(event.getPlayer().getUniqueId());
        duelManager.handleQuit(event.getPlayer());
    }
}
