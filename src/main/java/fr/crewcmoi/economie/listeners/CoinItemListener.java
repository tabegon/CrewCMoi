package fr.crewcmoi.economie.listeners;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import fr.crewcmoi.economie.managers.EconomyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import fr.crewcmoi.other.utils.MoneyFormat;

/**
 * Permet à un joueur de gagner de l'argent en faisant clic droit avec une "pièce" custom
 * (item ItemsAdder identifié par son id namespaced, ex: "crewcmoi:piece"). L'item est
 * identifié via l'API ItemsAdder (CustomStack) et non via son Custom Model Data brut, ce
 * qui reste fiable même si ItemsAdder change/réattribue les model data en interne.
 *
 * Un clic droit consomme une pièce (retire 1 du stack en main) et crédite le joueur du
 * montant configuré.
 */
public class CoinItemListener implements Listener {

    private final Main plugin;
    private final EconomyManager economyManager;

    private final boolean enabled;
    private final String coinItemId;
    private final double coinValue;

    public CoinItemListener(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;

        this.enabled = plugin.getConfig().getBoolean("coin-item.enabled", true);
        this.coinItemId = plugin.getConfig().getString("coin-item.itemsadder-id", "server:ccoin");
        this.coinValue = plugin.getConfig().getDouble("coin-item.value", 50.0);
    }

    @EventHandler
    public void onCoinRightClick(PlayerInteractEvent event) {
        if (!enabled) {
            return;
        }

        // Ignore le clic gauche, les blocs, et évite de déclencher deux fois l'event
        // (une fois pour la main principale, une fois pour la main secondaire).
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            return;
        }

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null || !customStack.getNamespacedID().equalsIgnoreCase(coinItemId)) {
            return;
        }

        event.setCancelled(true);

        // Consomme une pièce du stack en main.
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        economyManager.deposit(player.getUniqueId(), coinValue);
        sendMessage(player);
    }

    private void sendMessage(Player player) {
        String message = plugin.getMessages().getString("coin-item.redeemed");
        if (message == null) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix", "");
        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");
        player.sendMessage((prefix + message)
                .replace("{amount}", MoneyFormat.format(coinValue) + currency)
                .replace('&', '§'));
    }
}
