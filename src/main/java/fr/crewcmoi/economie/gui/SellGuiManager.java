package fr.crewcmoi.economie.gui;

import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.economie.managers.PricesManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import fr.crewcmoi.other.utils.MoneyFormat;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.pvp.utils.HeadSellPrice;
import java.util.ArrayList;
import java.util.List;

public class SellGuiManager {

    private final Main plugin;
    private final PricesManager pricesManager;
    private final EconomyManager economyManager;

    public SellGuiManager(Main plugin, PricesManager pricesManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.pricesManager = pricesManager;
        this.economyManager = economyManager;
    }

    private double getSellPrice(ItemStack stack) {
        double taggedPrice = HeadSellPrice.getPrice(plugin, stack);
        if (taggedPrice > 0) {
            return taggedPrice;
        }

        double basePrice = pricesManager.getPrice(stack.getType());
        double contentValue = getShulkerContentValue(stack);

        if (basePrice <= 0 && contentValue <= 0) {
            return -1.0;
        }
        return Math.max(basePrice, 0.0) + contentValue;
    }

    private double getShulkerContentValue(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return 0.0;
        }
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            return 0.0;
        }

        double total = 0.0;
        for (ItemStack content : shulkerBox.getInventory().getContents()) {
            if (content == null || content.getType() == Material.AIR) {
                continue;
            }
            double unitPrice = getSellPrice(content);
            if (unitPrice > 0) {
                total += unitPrice * content.getAmount();
            }
        }
        return total;
    }

    public void open(Player player) {
        SellHolder holder = new SellHolder();
        Inventory gui = Bukkit.createInventory(holder, SellHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', "&2&lᴠᴇɴᴛᴇ ᴅ'ᴏʙᴊᴇᴛꜱ"));
        holder.setInventory(gui);

        renderNormalState(gui);

        player.openInventory(gui);
    }

    private void renderNormalState(Inventory gui) {
        ItemStack filler = createFiller();
        for (int i = SellHolder.ITEM_SLOTS.length; i < SellHolder.SIZE; i++) {
            gui.setItem(i, filler);
        }
        gui.setItem(SellHolder.SELL_SLOT, createConfirmButton(gui));
    }

    private ItemStack createFiller() {
        return GuiItems.nothing(" ");
    }

    private ItemStack createConfirmButton(Inventory gui) {
        String currency = plugin.getConfig().getString("economy.currency-symbol");
        double total = 0.0;
        int itemCount = 0;
        boolean hasUnsellable = false;

        for (int slot : SellHolder.ITEM_SLOTS) {
            ItemStack stack = gui.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            double unitPrice = getSellPrice(stack);
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
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lᴠᴇɴᴅʀᴇ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7ᴘʟᴀᴄᴇᴢ ᴠᴏꜱ ᴏʙᴊᴇᴛꜱ ᴅᴀɴꜱ ʟᴇꜱ ᴇᴍᴘʟᴀᴄᴇᴍᴇɴᴛꜱ"));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7ᴄɪ-ᴅᴇꜱꜱᴜꜱ ᴘᴜɪꜱ ᴄʟɪǫᴜᴇᴢ ɪᴄɪ ᴘᴏᴜʀ ᴠᴇɴᴅʀᴇ."));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7ᴏʙᴊᴇᴛꜱ ᴠᴇɴᴅᴀʙʟᴇꜱ : &e" + itemCount));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    "&7ᴠᴏᴜꜱ ᴀʟʟᴇᴢ ɢᴀɢɴᴇʀ : &a" + MoneyFormat.format(total) + currency));
            if (hasUnsellable) {
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&8(ᴄᴇʀᴛᴀɪɴꜱ ᴏʙᴊᴇᴛꜱ ᴘʟᴀᴄᴇꜱ ɴᴇ ꜱᴏɴᴛ ᴘᴀꜱ ᴠᴇɴᴅᴀʙʟᴇꜱ)"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void refreshConfirmButton(Inventory gui) {
        gui.setItem(SellHolder.SELL_SLOT, createConfirmButton(gui));
    }

    public void askConfirmation(Player player, Inventory gui, SellHolder holder) {
        List<ItemStack> snapshot = new ArrayList<>();
        double total = 0.0;
        int itemCount = 0;

        for (int slot : SellHolder.ITEM_SLOTS) {
            ItemStack stack = gui.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            snapshot.add(stack.clone());
            double unitPrice = getSellPrice(stack);
            if (unitPrice > 0) {
                total += unitPrice * stack.getAmount();
                itemCount += stack.getAmount();
            }
        }

        if (snapshot.isEmpty()) {
            Messages.send(player, "economy.sell.gui-no-sellable-items", java.util.Map.of(), false);
            return;
        }

        holder.setPendingItems(snapshot);
        holder.setConfirming(true);

        String currency = plugin.getConfig().getString("economy.currency-symbol");
        ItemStack filler = createFiller();

        for (int slot : SellHolder.ITEM_SLOTS) {
            gui.setItem(slot, filler);
        }
        for (int i = SellHolder.ITEM_SLOTS.length; i < SellHolder.SIZE; i++) {
            gui.setItem(i, filler);
        }

        ItemStack confirmButton = GuiItems.checkmarkButton("&a&lᴄᴏɴꜰɪʀᴍᴇʀ ʟᴀ ᴠᴇɴᴛᴇ");
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        if (confirmMeta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴏʙᴊᴇᴛꜱ ᴠᴇɴᴅᴀʙʟᴇꜱ : &e" + itemCount));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴠᴏᴜꜱ ᴀʟʟᴇᴢ ɢᴀɢɴᴇʀ : &a" + MoneyFormat.format(total) + currency));
            confirmMeta.setLore(lore);
            confirmButton.setItemMeta(confirmMeta);
        }
        gui.setItem(SellHolder.CONFIRM_SLOT, confirmButton);

        ItemStack cancelButton = GuiItems.cancelButton("&c&lᴀɴɴᴜʟᴇʀ");
        gui.setItem(SellHolder.CANCEL_CONFIRM_SLOT, cancelButton);
    }

    public void cancelConfirmation(Inventory gui, SellHolder holder) {
        List<ItemStack> pending = holder.getPendingItems();
        holder.setConfirming(false);
        holder.setPendingItems(null);

        for (int slot : SellHolder.ITEM_SLOTS) {
            gui.setItem(slot, null);
        }

        if (pending != null) {
            int index = 0;
            for (ItemStack stack : pending) {
                if (index >= SellHolder.ITEM_SLOTS.length) {
                    break;
                }
                gui.setItem(SellHolder.ITEM_SLOTS[index], stack);
                index++;
            }
        }

        renderNormalState(gui);
    }

    public void confirmSale(Player player, Inventory gui, SellHolder holder) {
        List<ItemStack> pending = holder.getPendingItems();
        holder.setConfirming(false);
        holder.setPendingItems(null);

        if (pending == null || pending.isEmpty()) {
            player.closeInventory();
            return;
        }

        double total = 0.0;
        int itemsSold = 0;
        String currency = plugin.getConfig().getString("economy.currency-symbol");

        for (ItemStack stack : pending) {
            double unitPrice = getSellPrice(stack);
            if (unitPrice <= 0) {

                returnSingleItem(player, stack);
                continue;
            }
            total += unitPrice * stack.getAmount();
            itemsSold += stack.getAmount();
        }

        if (itemsSold > 0) {
            economyManager.deposit(player.getUniqueId(), total);
            Messages.send(player, "economy.sell.gui-sold", java.util.Map.of("count", itemsSold, "amount", MoneyFormat.format(total) + currency), false);
        } else {
            Messages.send(player, "economy.sell.gui-no-sellable-items", java.util.Map.of(), false);
        }

        for (int slot : SellHolder.ITEM_SLOTS) {
            gui.setItem(slot, null);
        }

        player.closeInventory();
    }

    private void returnSingleItem(Player player, ItemStack stack) {
        for (ItemStack leftover : player.getInventory().addItem(stack).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    public void returnItems(Player player, Inventory gui, SellHolder holder) {
        if (holder.isConfirming() && holder.getPendingItems() != null) {
            for (ItemStack stack : holder.getPendingItems()) {
                returnSingleItem(player, stack);
            }
            holder.setPendingItems(null);
            holder.setConfirming(false);
            return;
        }

        for (int slot : SellHolder.ITEM_SLOTS) {
            ItemStack stack = gui.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            gui.setItem(slot, null);
            returnSingleItem(player, stack);
        }
    }
}
