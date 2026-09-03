package fr.crewcmoi.pvp.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.DuelManager;
import fr.crewcmoi.other.utils.GuiItems;
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
 * Construit et rafraîchit la GUI /duel <joueur> : permet au demandeur de choisir
 * si l'inventaire est conservé en cas de défaite (keepinventory) et le montant
 * d'argent mis en jeu, avant de confirmer et d'envoyer la demande à l'adversaire.
 */
public class DuelConfigGuiManager {

    private final Main plugin;
    private final DuelManager duelManager;

    public DuelConfigGuiManager(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
    }

    public void open(Player requester, Player target) {
        DuelConfigHolder holder = new DuelConfigHolder(target.getUniqueId(), target.getName());
        Inventory gui = Bukkit.createInventory(holder, DuelConfigHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', "&8&lDuel contre " + target.getName()));
        holder.setInventory(gui);
        render(holder);
        requester.openInventory(gui);
    }

    public void render(DuelConfigHolder holder) {
        Inventory gui = holder.getInventory();
        ItemStack filler = GuiItems.nothing(" ");
        for (int i = 0; i < DuelConfigHolder.SIZE; i++) {
            gui.setItem(i, filler);
        }

        gui.setItem(DuelConfigHolder.INFO_SLOT, buildInfoItem(holder));
        gui.setItem(DuelConfigHolder.KEEPINVENTORY_SLOT, buildKeepInventoryItem(holder));
        gui.setItem(DuelConfigHolder.BET_SLOT, buildBetItem(holder));
        gui.setItem(DuelConfigHolder.KIT_SLOT, buildKitItem(holder));
        gui.setItem(DuelConfigHolder.DROPHEAD_SLOT, buildDropHeadItem(holder));
        gui.setItem(DuelConfigHolder.CONFIRM_SLOT, GuiItems.checkmarkButton("&a&lᴇɴᴠᴏʏᴇʀ ʟᴀ ᴅᴇᴍᴀɴᴅᴇ"));
        gui.setItem(DuelConfigHolder.CANCEL_SLOT, GuiItems.cancelButton("&c&lᴀɴɴᴜʟᴇʀ"));
    }

    private ItemStack buildInfoItem(DuelConfigHolder holder) {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lDuel contre " + holder.getTargetName()));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Configurez les règles du duel puis"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7cliquez sur \"Envoyer la demande\"."));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7" + holder.getTargetName() + " devra accepter"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7pour que le duel commence."));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildKeepInventoryItem(DuelConfigHolder holder) {
        ItemStack item = new ItemStack(holder.isKeepInventory() ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lKeepinventory"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Le perdant garde son inventaire : "
                + (holder.isKeepInventory() ? "&a&loui" : "&c&lnon")));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eClic gauche pour changer"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBetItem(DuelConfigHolder holder) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&lArgent en jeu"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Mise actuelle : &e" + MoneyFormat.format(holder.getBet())));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eClic gauche &7: +" + MoneyFormat.format(duelManager.getBetStep())));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eClic droit &7: -" + MoneyFormat.format(duelManager.getBetStep())));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eClic molette &7: entrer un montant exact"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildKitItem(DuelConfigHolder holder) {
        boolean selected = holder.getKitId() != null;
        ItemStack item = new ItemStack(selected ? Material.NETHERITE_CHESTPLATE : Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', selected
                ? "&6&lKit : &eKit Classique"
                : "&6&lKit de duel"));
        List<String> lore = new ArrayList<>();
        if (selected) {
            double price = duelManager.getDuelKitManager().getPrice(holder.getKitId());
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Prix par joueur : &e" + MoneyFormat.format(price)));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&aLe kit est sélectionné."));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&cKeepinventory est automatiquement désactivé."));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eClic gauche pour retirer le kit"));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Kit Classique"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Prix : &e" + MoneyFormat.format(duelManager.getDuelKitManager().getPrice("basic"))));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Armure Protection IV, Mending, Unbreaking III"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Épée Sharpness V + Fire Aspect II"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Mace Density V + Wind Burst III"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Hache Sharpness V + bouclier enchanté"));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eClic gauche pour sélectionner"));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildDropHeadItem(DuelConfigHolder holder) {
        ItemStack item = new ItemStack(holder.isDropHead() ? Material.PLAYER_HEAD : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lTête du perdant"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Le perdant drop sa tête (vendable) : "
                + (holder.isDropHead() ? "&a&loui" : "&c&lnon")));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eClic gauche pour changer"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
