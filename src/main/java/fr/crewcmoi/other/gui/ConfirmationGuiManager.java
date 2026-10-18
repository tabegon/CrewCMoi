package fr.crewcmoi.other.gui;

import fr.crewcmoi.other.utils.GuiItems;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ConfirmationGuiManager {

    public void open(Player player, String title, ItemStack infoItem, List<String> extraLore,
                     Runnable onConfirm, Runnable onCancel) {
        open(player, title, infoItem, extraLore, null, null, onConfirm, onCancel);
    }

    public void open(Player player, String title, ItemStack infoItem, List<String> extraLore,
                     ItemStack sellerItem, ItemStack moneyItem, Runnable onConfirm, Runnable onCancel) {
        ConfirmationHolder holder = new ConfirmationHolder();
        Inventory gui = Bukkit.createInventory(holder, ConfirmationHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', title));
        holder.setInventory(gui);
        holder.setOnConfirm(onConfirm);
        holder.setOnCancel(onCancel);

        ItemStack filler = GuiItems.nothing(" ");
        for (int i = 0; i < ConfirmationHolder.SIZE; i++) {
            gui.setItem(i, filler.clone());
        }
        ItemStack itemsAdderFiller = itemsAdderItem("server:fill_button", new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE));
        for (int slot : new int[]{12, 13, 14, 21, 23, 30, 31, 32}) {
            gui.setItem(slot, itemsAdderFiller.clone());
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

        if (sellerItem != null) {
            gui.setItem(ConfirmationHolder.SELLER_SLOT, sellerItem);
        }
        if (moneyItem != null) {
            gui.setItem(ConfirmationHolder.MONEY_SLOT, moneyItem);
        }

        gui.setItem(ConfirmationHolder.CANCEL_SLOT,
                GuiItems.cancelButton("&c&lᴀɴɴᴜʟᴇʀ"));
        gui.setItem(ConfirmationHolder.CONFIRM_SLOT,
                GuiItems.checkmarkButton("&a&lᴀᴄʜᴇᴛᴇʀ"));

        player.openInventory(gui);
    }

    private ItemStack itemsAdderItem(String id, ItemStack fallback) {
        try {
            CustomStack custom = CustomStack.getInstance(id);
            if (custom != null && custom.getItemStack() != null) return custom.getItemStack().clone();
        } catch (Throwable ignored) {
        }
        return fallback.clone();
    }
}
