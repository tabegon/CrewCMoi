package fr.crewcmoi.pvp.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.MoneyFormat;
import fr.crewcmoi.pvp.managers.DuelManager;
import fr.crewcmoi.pvp.utils.HeadSellPrice;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerHeadDropListener implements Listener {

    private final Main plugin;
    private final DuelManager duelManager;

    public PlayerHeadDropListener(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
    }

    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("player-head-drop.enabled", true)) {
            return;
        }

        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {

            return;
        }
        if (duelManager.getSession(victim.getUniqueId()) != null) {

            return;
        }

        double price = plugin.getConfig().getDouble("player-head-drop.price", 250.0);
        ItemStack head = createHead(plugin, victim, price);
        if (head != null) {
            victim.getWorld().dropItemNaturally(victim.getLocation(), head);
        }
    }

    public static ItemStack createHead(Main plugin, Player victim, double price) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return null;
        }

        meta.setOwningPlayer(victim);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fTête de &e" + victim.getName()));

        String currency = plugin.getConfig().getString("economy.currency-symbol");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&',
                "&7Prix de vente : &a" + MoneyFormat.format(price) + currency));
        meta.setLore(lore);

        HeadSellPrice.apply(plugin, meta, price);
        head.setItemMeta(meta);
        return head;
    }
}
