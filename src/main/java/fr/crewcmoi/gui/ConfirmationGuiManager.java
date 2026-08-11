package fr.crewcmoi.gui;

import fr.crewcmoi.utils.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit une GUI générique de confirmation (oui/non), utilisée avant un
 * achat via /ah ou une vente via /sell afin d'éviter les clics accidentels.
 */
public class ConfirmationGuiManager {

    public void open(Player player, String title, ItemStack infoItem, List<String> extraLore,
                      Runnable onConfirm, Runnable onCancel) {
        ConfirmationHolder holder = new ConfirmationHolder();
        Inventory gui = Bukkit.createInventory(holder, ConfirmationHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', title));
        holder.setInventory(gui);
        holder.setOnConfirm(onConfirm);
        holder.setOnCancel(onCancel);

        ItemStack filler = createFiller();
        for (int i = 0; i < ConfirmationHolder.SIZE; i++) {
            gui.setItem(i, filler);
        }

        if (infoItem != null) {
            ItemStack display = infoItem.clone();
            if (extraLore != null && !extraLore.isEmpty()) {
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    List<String> lore = new ArrayList<>();
                    if (meta.hasLore() && meta.getLore() != null) {
                        lore.addAll(meta.getLore());
                    }
                    lore.addAll(extraLore);
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
            }
            gui.setItem(ConfirmationHolder.INFO_SLOT, display);
        }

        gui.setItem(ConfirmationHolder.CONFIRM_SLOT,
                GuiItems.checkmarkButton("&a&lᴄᴏɴꜰɪʀᴍᴇʀ"));
        gui.setItem(ConfirmationHolder.CANCEL_SLOT,
                GuiItems.cancelButton("&c&lᴀɴɴᴜʟᴇʀ"));

        player.openInventory(gui);
    }

    private ItemStack createFiller() {
        return GuiItems.nothing(" ");
    }
}
