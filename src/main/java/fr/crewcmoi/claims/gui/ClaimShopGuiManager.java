package fr.crewcmoi.claims.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.managers.ClaimManager;
import fr.crewcmoi.other.utils.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit et rafraîchit la GUI /claim shop : permet d'acheter des claims supplémentaires
 * en plus du quota gratuit, contre de l'argent (économie du serveur).
 */
public class ClaimShopGuiManager {

    private final Main plugin;
    private final ClaimManager claimManager;

    public ClaimShopGuiManager(Main plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    public void open(Player player) {
        ClaimShopHolder holder = new ClaimShopHolder();
        Inventory gui = Bukkit.createInventory(holder, ClaimShopHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', "&8&lBoutique de claims"));
        holder.setInventory(gui);
        render(player, holder);
        player.openInventory(gui);
    }

    public void render(Player player, ClaimShopHolder holder) {
        Inventory gui = holder.getInventory();
        for (int i = 0; i < ClaimShopHolder.SIZE; i++) {
            gui.setItem(i, null);
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");
        int base = plugin.getConfig().getInt("claims.max-per-player", 0);
        int extra = claimManager.getExtraClaims(player.getUniqueId());
        int maxExtra = plugin.getConfig().getInt("claims.shop.max-extra", 10);
        double price = claimManager.getNextShopPrice(player);

        ItemStack buy = new ItemStack(extra >= maxExtra ? Material.BARRIER : Material.EMERALD);
        ItemMeta meta = buy.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lAcheter un claim supplémentaire"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Claims actuels : &e" + (base + extra)
                + " &7(&e" + base + " &7gratuits + &e" + extra + " &7achetés)"));
        lore.add("");
        if (extra >= maxExtra) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&cVous avez atteint la limite de claims achetables."));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Prix : &a" + MoneyFormat.format(price) + currency));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eCliquez pour acheter"));
        }
        meta.setLore(lore);
        buy.setItemMeta(meta);
        gui.setItem(ClaimShopHolder.BUY_SLOT, buy);
    }
}
