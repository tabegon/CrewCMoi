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
        this.coinItemId = plugin.getConfig().getString("coin-item.itemsadder-id");
        this.coinValue = plugin.getConfig().getDouble("coin-item.value", 50.0);
    }

    @EventHandler
    public void onCoinRightClick(PlayerInteractEvent event) {
        if (!enabled) {
            return;
        }

        
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
        String prefix = plugin.getMessages().getString("prefix");
        String currency = plugin.getConfig().getString("economy.currency-symbol");
        player.sendMessage((prefix + message)
                .replace("{amount}", MoneyFormat.format(coinValue) + currency)
                .replace('&', '§'));
    }
}
