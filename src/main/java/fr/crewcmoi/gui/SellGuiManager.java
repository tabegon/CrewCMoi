package fr.crewcmoi.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.managers.EconomyManager;
import fr.crewcmoi.managers.PricesManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Construit la GUI de /sell et gère le calcul/la validation de la vente.
 */
public class SellGuiManager {

    private final Main plugin;
    private final PricesManager pricesManager;
    private final EconomyManager economyManager;
    private final DecimalFormat format = new DecimalFormat("#,##0.00");

    public SellGuiManager(Main plugin, PricesManager pricesManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.pricesManager = pricesManager;
        this.economyManager = economyManager;
    }

    public void open(Player player) {
        SellHolder holder = new SellHolder();
        Inventory gui = Bukkit.createInventory(holder, SellHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', "&2&lᴠᴇɴᴛᴇ ᴅ'ᴏʙᴊᴇᴛꜱ"));
        holder.setInventory(gui);

        ItemStack filler = createFiller();
        for (int i = 18; i < SellHolder.SIZE; i++) {
            gui.setItem(i, filler);
        }

        gui.setItem(SellHolder.CONFIRM_SLOT, createConfirmButton(gui));

        player.openInventory(gui);
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Construit le bouton émeraude avec, dans son lore, le montant total
     * actuellement calculé pour les objets présents dans la GUI.
     */
    private ItemStack createConfirmButton(Inventory gui) {
        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");
        double total = 0.0;
        int itemCount = 0;
        boolean hasUnsellable = false;

        for (int slot : SellHolder.ITEM_SLOTS) {
            ItemStack stack = gui.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            double unitPrice = pricesManager.getPrice(stack.getType());
            if (unitPrice <= 0) {
                hasUnsellable = true;
                continue;
            }
            total += unitPrice * stack.getAmount();
            itemCount += stack.getAmount();
        }

        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lᴄᴏɴꜰɪʀᴍᴇʀ ʟᴀ ᴠᴇɴᴛᴇ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7ᴘʟᴀᴄᴇᴢ ᴠᴏꜱ ᴏʙᴊᴇᴛꜱ ᴅᴀɴꜱ ʟᴇꜱ ᴇᴍᴘʟᴀᴄᴇᴍᴇɴᴛꜱ"));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7ᴄɪ-ᴅᴇꜱꜱᴜꜱ ᴘᴜɪꜱ ᴄʟɪǫᴜᴇᴢ ɪᴄɪ ᴘᴏᴜʀ ᴠᴇɴᴅʀᴇ."));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7ᴏʙᴊᴇᴛꜱ ᴠᴇɴᴅᴀʙʟᴇꜱ : &e" + itemCount));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7ᴠᴏᴜꜱ ᴀʟʟᴇᴢ ɢᴀɢɴᴇʀ : &a" + format.format(total) + currency));
            if (hasUnsellable) {
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&8(ᴄᴇʀᴛᴀɪɴꜱ ᴏʙᴊᴇᴛꜱ ᴘʟᴀᴄᴇꜱ ɴᴇ ꜱᴏɴᴛ ᴘᴀꜱ ᴠᴇɴᴅᴀʙʟᴇꜱ)"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Recalcule et remet à jour le bouton émeraude (montant à gagner) après
     * chaque ajout/retrait d'objet dans la GUI de vente.
     */
    public void refreshConfirmButton(Inventory gui) {
        gui.setItem(SellHolder.CONFIRM_SLOT, createConfirmButton(gui));
    }

    /**
     * Calcule et effectue la vente des objets placés dans la GUI.
     * Retire les objets vendables, laisse les objets non vendables,
     * et retourne le montant total gagné.
     */
    public double sell(Player player, Inventory gui) {
        double total = 0.0;
        int itemsSold = 0;
        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

        for (int slot : SellHolder.ITEM_SLOTS) {
            ItemStack stack = gui.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            double unitPrice = pricesManager.getPrice(stack.getType());
            if (unitPrice <= 0) {
                // Objet non vendable : on le laisse dans la GUI (rendu au joueur à la fermeture)
                continue;
            }

            total += unitPrice * stack.getAmount();
            itemsSold += stack.getAmount();
            gui.setItem(slot, null);
        }

        if (itemsSold > 0) {
            economyManager.deposit(player.getUniqueId(), total);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&8[&6ᴇᴄᴏɴᴏᴍʏ&8] &r&aᴠᴇɴᴛᴇ ᴇꜰꜰᴇᴄᴛᴜᴇᴇ : &e" + itemsSold + " ᴏʙᴊᴇᴛ(ꜱ)&a ᴘᴏᴜʀ &e"
                            + format.format(total) + currency + "&a."));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&8[&6ᴇᴄᴏɴᴏᴍʏ&8] &r&cᴀᴜᴄᴜɴ ᴏʙᴊᴇᴛ ᴠᴇɴᴅᴀʙʟᴇ ᴛʀᴏᴜᴠᴇ ᴅᴀɴꜱ ʟᴀ ɢᴜɪ."));
        }

        return total;
    }

    /**
     * Rend au joueur les objets restants dans la GUI (appelé à la fermeture de l'inventaire).
     */
    public void returnItems(Player player, Inventory gui) {
        for (int slot : SellHolder.ITEM_SLOTS) {
            ItemStack stack = gui.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            gui.setItem(slot, null);

            for (ItemStack leftover : player.getInventory().addItem(stack).values()) {
                player.getWorld().dropItem(player.getLocation(), leftover);
            }
        }
    }
}
