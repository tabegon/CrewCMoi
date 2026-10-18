package fr.crewcmoi.economie.gui;

import fr.crewcmoi.other.utils.Messages;
import fr.crewcmoi.other.utils.ConfigGuiItem;

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
import org.bukkit.configuration.ConfigurationSection;

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

        double basePrice = pricesManager.getPrice(stack);
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

        // Tous les slots sauf ceux réservés au dépôt sont décorés.
        // Les slots 25, 32, 33 et 34 doivent être réellement vides dans
        // l'écran de vente normal (ils ne doivent notamment pas conserver
        // le bouton de confirmation après un retour depuis la confirmation).
        for (int i = 0; i < SellHolder.SIZE; i++) {
            if (!SellHolder.isItemSlot(i)) {
                gui.setItem(i, filler.clone());
            }
        }
        ConfigurationSection slots = plugin.getConfig().getConfigurationSection("sell-gui.slots");
        if (slots != null) {
            for (String key : slots.getKeys(false)) {
                int slot;
                try {
                    slot = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    plugin.getLogger().warning("Sell GUI : slot invalide '" + key + "'.");
                    continue;
                }

                if (SellHolder.isItemSlot(slot) || slot < 0 || slot >= SellHolder.SIZE) {
                    // Les slots de dépôt doivent rester libres pour le joueur.
                    continue;
                }

                ConfigurationSection section = slots.getConfigurationSection(key);
                if (section != null) {
                    gui.setItem(slot, ConfigGuiItem.fromSection(plugin, section, Material.GRAY_STAINED_GLASS_PANE));
                }
            }
        }

        // Bouton NPC fixe (Custom Model Data 33 via GuiItems).
        gui.setItem(SellHolder.NPC_SLOT, GuiItems.npc("&f"));

        // Bouton vendre fixe : Custom Model Data 18 via GuiItems.
        gui.setItem(SellHolder.SELL_SLOT, GuiItems.checkmarkButton("&a&lᴠᴇɴᴅʀᴇ"));

        // Bouton Search fixe (Custom Model Data 16 via GuiItems).
        renderSearchButtons(gui, false);

        // Ces quatre emplacements doivent toujours utiliser le modèle ItemsAdder demandé.
        ItemStack sellBotFiller = itemsAdderItem("server:fill_button", new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        for (int emptySlot : new int[]{25, 32, 33, 34}) {
            gui.setItem(emptySlot, sellBotFiller.clone());
        }
    }

    private int[] getSearchSlots() {
        return new int[] {46};
    }

    private boolean isIntentionallyEmptySlot(int slot) {
        return slot == 25 || slot == 32 || slot == 33 || slot == 34;
    }

    private int[] getSearchItemSlots() {
        List<Integer> slots = plugin.getConfig().getIntegerList("sell-gui.buttons.search.item-slots");
        if (slots == null || slots.isEmpty()) {
            return SellHolder.ITEM_SLOTS.clone();
        }
        return slots.stream().filter(SellHolder::isItemSlot).distinct().mapToInt(Integer::intValue).toArray();
    }

    private double calculateSearchTotal(Inventory gui) {
        double total = 0.0;
        for (int slot : getSearchItemSlots()) {
            ItemStack stack = gui.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            double unitPrice = getSellPrice(stack);
            if (unitPrice > 0) {
                total += unitPrice * stack.getAmount();
            }
        }
        return total;
    }

    private ItemStack createSearchButton(Inventory gui, boolean calculated) {
        double total = calculateSearchTotal(gui);
        ItemStack item = GuiItems.searchButton("&6&lRECHERCHE");
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        List<String> lore = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("sell-gui.buttons.search");
        if (section != null && section.isList("lore")) {
            for (String line : section.getStringList("lore")) {
                lore.add(replaceSearchPlaceholders(line, total, calculated));
            }
        } else {
            lore.add(ChatColor.GRAY + "Cliquez pour calculer le prix total.");
            lore.add(ChatColor.GRAY + "Prix total : " + ChatColor.GOLD + MoneyFormat.format(total));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String replaceSearchPlaceholders(String text, double total, boolean calculated) {
        String currency = plugin.getConfig().getString("economy.currency-symbol", "");
        String totalText = MoneyFormat.format(total) + currency;
        if (calculated) {
            totalText = ChatColor.GOLD + totalText + ChatColor.RESET;
        }
        return ChatColor.translateAlternateColorCodes('&', text)
                .replace("%total%", totalText)
                .replace("%search-total%", totalText)
                .replace("%currency%", currency);
    }

    private void renderSearchButtons(Inventory gui, boolean calculated) {
        for (int slot : getSearchSlots()) {
            if (slot < 0 || slot >= SellHolder.SIZE) {
                continue;
            }
            if (slot == SellHolder.SELL_SLOT || slot == SellHolder.CONFIRM_SLOT || slot == SellHolder.CANCEL_CONFIRM_SLOT) {
                plugin.getLogger().warning("Sell GUI : le slot search " + slot + " entre en conflit avec un bouton existant.");
                continue;
            }
            gui.setItem(slot, createSearchButton(gui, calculated));
        }
    }

    public void calculateAndDisplaySearch(Inventory gui) {
        renderSearchButtons(gui, true);
    }

    public boolean isSearchSlot(int slot) {
        for (int searchSlot : getSearchSlots()) {
            if (searchSlot == slot) {
                return true;
            }
        }
        return false;
    }

    private ItemStack createFiller() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("sell-gui.filler");
        if (section != null) {
            return ConfigGuiItem.fromSection(plugin, section, Material.GRAY_STAINED_GLASS_PANE);
        }
        return GuiItems.nothing(" ");
    }

    private ItemStack createConfiguredButton(String path, Material fallbackMaterial,
                                             String defaultName, List<String> defaultLore,
                                             int itemCount, double total) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        ItemStack item;

        if (section != null) {
            item = ConfigGuiItem.fromSection(plugin, section, fallbackMaterial);

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (section.isList("lore")) {
                    List<String> lore = new ArrayList<>();
                    for (String line : section.getStringList("lore")) {
                        lore.add(replacePlaceholders(line, itemCount, total));
                    }
                    meta.setLore(lore);
                } else if (!defaultLore.isEmpty()) {
                    List<String> lore = new ArrayList<>();
                    for (String line : defaultLore) {
                        lore.add(ChatColor.translateAlternateColorCodes('&',
                                replacePlaceholders(line, itemCount, total)));
                    }
                    meta.setLore(lore);
                }
                item.setItemMeta(meta);
            }
            return item;
        }

        ItemStack fallback = new ItemStack(fallbackMaterial);
        ItemMeta meta = fallback.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', defaultName));
            List<String> lore = new ArrayList<>();
            for (String line : defaultLore) {
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        replacePlaceholders(line, itemCount, total)));
            }
            meta.setLore(lore);
            fallback.setItemMeta(meta);
        }
        return fallback;
    }

    private String replacePlaceholders(String text, int itemCount, double total) {
        String currency = plugin.getConfig().getString("economy.currency-symbol", "");
        return text
                .replace("%item-count%", String.valueOf(itemCount))
                .replace("%total%", MoneyFormat.format(total))
                .replace("%currency%", currency);
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

        List<String> defaultLore = new ArrayList<>();
        defaultLore.add("&7ᴏʙᴊᴇᴛꜱ ᴠᴇɴᴅᴀʙʟᴇꜱ : &e%item-count%");
        defaultLore.add("&7ᴠᴏᴜꜱ ᴀʟʟᴇᴢ ɢᴀɢɴᴇʀ : &a%total%%currency%");
        if (hasUnsellable) {
            defaultLore.add("&8(ᴄᴇʀᴛᴀɪɴꜱ ᴏʙᴊᴇᴛꜱ ᴘʟᴀᴄᴇꜱ ɴᴇ ꜱᴏɴᴛ ᴘᴀꜱ ᴠᴇɴᴅᴀʙʟᴇꜱ)");
        }

        ItemStack sellButton = GuiItems.checkmarkButton("&a&lᴠᴇɴᴅʀᴇ");
        ItemMeta meta = sellButton.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            for (String line : defaultLore) {
                lore.add(ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, itemCount, total)));
            }
            meta.setLore(lore);
            sellButton.setItemMeta(meta);
        }
        return sellButton;
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
        // En confirmation, les anciens slots volontairement vides du Sell Bot
        // redeviennent des NOTHING comme le reste du fond. Le slot 33 sera
        // ensuite remplacé par le checkmark de confirmation.
        for (int i = 0; i < SellHolder.SIZE; i++) {
            if (!SellHolder.isItemSlot(i)) {
                gui.setItem(i, filler.clone());
            }
        }

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("&7ᴏʙᴊᴇᴛꜱ ᴠᴇɴᴅᴀʙʟᴇꜱ : &e%item-count%");
        confirmLore.add("&7ᴠᴏᴜꜱ ᴀʟʟᴇᴢ ɢᴀɢɴᴇʀ : &a%total%%currency%");

        ItemStack confirmButton = GuiItems.checkmarkButton("&a&lᴄᴏɴꜰɪʀᴍᴇʀ ʟᴀ ᴠᴇɴᴛᴇ");
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        if (confirmMeta != null) {
            List<String> lore = new ArrayList<>();
            for (String line : confirmLore) {
                lore.add(ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, itemCount, total)));
            }
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

    private ItemStack itemsAdderItem(String id, ItemStack fallback) {
        try {
            dev.lone.itemsadder.api.CustomStack custom = dev.lone.itemsadder.api.CustomStack.getInstance(id);
            if (custom != null && custom.getItemStack() != null) return custom.getItemStack().clone();
        } catch (Throwable ignored) {
        }
        return fallback;
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
