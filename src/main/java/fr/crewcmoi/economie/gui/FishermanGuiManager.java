package fr.crewcmoi.economie.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.other.utils.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gère la GUI du NPC pêcheur (clic droit sur le NPC configuré via
 * "fisherman.npc-id") : un menu avec deux boutons "Vendre"/"Acheter".
 * <p>
 * - Vendre : uniquement du poisson (brut/cuit/pufferfish) et des cannes à
 *   pêche, à des prix plus élevés qu'au /sell classique (voir
 *   "fisherman.sell-prices" dans config.yml).
 * - Acheter : un catalogue fixe de 5 objets (voir {@link #BUY_OFFERS}), prix
 *   configurables dans "fisherman.buy-prices".
 */
public class FishermanGuiManager {

    /**
     * Une offre du catalogue d'achat : matériau, enchantement optionnel (pour
     * la canne à pêche Chance des Flots 1) et clé de config pour le prix.
     */
    private record BuyOffer(String id, String displayName, Material material,
                             Enchantment enchantment, int enchantLevel, String priceConfigKey, double defaultPrice) {
    }

    private static final List<BuyOffer> BUY_OFFERS = List.of(
            new BuyOffer("cooked_cod", "&fCabillaud cuit", Material.COOKED_COD, null, 0, "cooked-cod", 20),
            new BuyOffer("cooked_salmon", "&fSaumon cuit", Material.COOKED_SALMON, null, 0, "cooked-salmon", 20),
            new BuyOffer("pufferfish", "&fPoisson-globe", Material.PUFFERFISH, null, 0, "pufferfish", 30),
            new BuyOffer("fishing_rod", "&fCanne à pêche", Material.FISHING_ROD, null, 0, "fishing-rod", 100),
            new BuyOffer("fishing_rod_lotr1", "&fCanne à pêche &e(Luck of the sea I)",
                    Material.FISHING_ROD, Enchantment.LUCK_OF_THE_SEA, 1, "fishing-rod-luck-of-the-sea-1", 150)
    );

    private final Main plugin;
    private final EconomyManager economyManager;
    private final Map<Material, Double> sellPrices = new LinkedHashMap<>();

    public FishermanGuiManager(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        loadSellPrices();
    }

    public void reload() {
        loadSellPrices();
    }

    private void loadSellPrices() {
        sellPrices.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("fisherman.sell-prices");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                plugin.getLogger().warning("config.yml (fisherman.sell-prices) : matériau inconnu \"" + key + "\" ignoré.");
                continue;
            }
            sellPrices.put(material, section.getDouble(key, 0.0));
        }
    }

    private double getBuyPrice(BuyOffer offer) {
        return plugin.getConfig().getDouble("fisherman.buy-prices." + offer.priceConfigKey(), offer.defaultPrice());
    }

    private String currency() {
        return plugin.getConfig().getString("economy.currency-symbol", "§f");
    }

    // -------------------------------------------------------------------
    // Menu principal
    // -------------------------------------------------------------------

    public void openMenu(Player player) {
        FishermanMenuHolder holder = new FishermanMenuHolder();
        Inventory gui = Bukkit.createInventory(holder, FishermanMenuHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', "&b&lꜰɪʜ ꜱᴇʟʟᴇʀ"));
        holder.setInventory(gui);

        ItemStack filler = GuiItems.nothing(" ");
        for (int i = 0; i < FishermanMenuHolder.SIZE; i++) {
            gui.setItem(i, filler);
        }

        gui.setItem(FishermanMenuHolder.SELL_BUTTON_SLOT, buildMenuButton(Material.COD,
                "&a&lᴠᴇɴᴅʀᴇ",
                List.of("&7ᴠᴇɴᴅꜱ ᴛᴏɴ ᴘᴏɪꜱꜱᴏɴ ᴇᴛ ᴛᴇꜱ ᴄᴀɴɴᴇꜱ",
                        "&7à ᴘᴇᴄʜᴇ, à ᴍᴇɪʟʟᴇᴜʀ ᴘʀɪx ǫᴜ'ᴀɪʟʟᴇᴜʀꜱ.")));
        gui.setItem(FishermanMenuHolder.BUY_BUTTON_SLOT, buildMenuButton(Material.FISHING_ROD,
                "&e&lᴀᴄʜᴇᴛᴇʀ",
                List.of("&7ᴀᴄʜᴇ̀ᴛᴇ ᴅᴇ ʟᴀ ɴᴏᴜʀʀɪᴛᴜʀᴇ ᴇᴛ ᴅᴇꜱ",
                        "&7ᴄᴀɴɴᴇꜱ à ᴘᴇᴄʜᴇ.")));

        player.openInventory(gui);
    }

    private ItemStack buildMenuButton(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            List<String> colored = new ArrayList<>();
            for (String line : lore) {
                colored.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    // -------------------------------------------------------------------
    // Vente (poisson + cannes à pêche uniquement)
    // -------------------------------------------------------------------

    /**
     * Prix de vente unitaire au pêcheur, ou -1 si l'objet n'est pas accepté ici
     * (seuls le poisson et les cannes à pêche le sont, voir "fisherman.sell-prices").
     */
    private double getSellPrice(ItemStack stack) {
        return sellPrices.getOrDefault(stack.getType(), -1.0);
    }

    public void openSell(Player player) {
        FishermanSellHolder holder = new FishermanSellHolder();
        Inventory gui = Bukkit.createInventory(holder, FishermanSellHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', "&2&lᴠᴇɴᴛᴇ ᴀᴜ ᴘᴇᴄʜᴇᴜʀ"));
        holder.setInventory(gui);

        renderSellNormalState(gui);

        player.openInventory(gui);
    }

    private void renderSellNormalState(Inventory gui) {
        ItemStack filler = GuiItems.nothing(" ");
        for (int i = FishermanSellHolder.ITEM_SLOTS.length; i < FishermanSellHolder.SIZE; i++) {
            gui.setItem(i, filler);
        }
        gui.setItem(FishermanSellHolder.SELL_SLOT, createSellConfirmButton(gui));
        gui.setItem(FishermanSellHolder.BACK_SLOT, GuiItems.cancelButton("&c&l« ʀᴇᴛᴏᴜʀ »"));
    }

    private ItemStack createSellConfirmButton(Inventory gui) {
        double total = 0.0;
        int itemCount = 0;
        boolean hasUnsellable = false;

        for (int slot : FishermanSellHolder.ITEM_SLOTS) {
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
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘʟᴀᴄᴇᴢ ᴠᴏᴛʀᴇ ᴘᴏɪꜱꜱᴏɴ ᴏᴜ ᴠᴏꜱ"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴄᴀɴɴᴇꜱ à ᴘᴇ̂ᴄʜᴇ ᴄɪ-ᴅᴇꜱꜱᴜꜱ ᴘᴜɪꜱ"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴄʟɪǫᴜᴇᴢ ɪᴄɪ ᴘᴏᴜʀ ᴠᴇɴᴅʀᴇ."));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴏʙᴊᴇᴛꜱ ᴠᴇɴᴅᴀʙʟᴇꜱ : &e" + itemCount));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴠᴏᴜꜱ ᴀʟʟᴇᴢ ɢᴀɢɴᴇʀ : &a" + MoneyFormat.format(total) + currency()));
            if (hasUnsellable) {
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&8(ꜱᴇᴜʟꜱ ʟᴇ ᴘᴏɪꜱꜱᴏɴ ᴇᴛ ʟᴇꜱ ᴄᴀɴɴᴇꜱ à ᴘᴇ̂ᴄʜᴇ ꜱᴏɴᴛ ᴀᴄᴄᴇᴘᴛᴇ́ꜱ ɪᴄɪ)"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void refreshSellConfirmButton(Inventory gui) {
        gui.setItem(FishermanSellHolder.SELL_SLOT, createSellConfirmButton(gui));
    }

    public void askSellConfirmation(Player player, Inventory gui, FishermanSellHolder holder) {
        List<ItemStack> snapshot = new ArrayList<>();
        double total = 0.0;
        int itemCount = 0;

        for (int slot : FishermanSellHolder.ITEM_SLOTS) {
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
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cᴘʟᴀᴄᴇ ᴅ'ᴀʙᴏʀᴅ ᴅᴇꜱ ᴏʙᴊᴇᴛꜱ à ᴠᴇɴᴅʀᴇ."));
            return;
        }

        holder.setPendingItems(snapshot);
        holder.setConfirming(true);

        ItemStack filler = GuiItems.nothing(" ");
        for (int slot : FishermanSellHolder.ITEM_SLOTS) {
            gui.setItem(slot, filler);
        }
        for (int i = FishermanSellHolder.ITEM_SLOTS.length; i < FishermanSellHolder.SIZE; i++) {
            gui.setItem(i, filler);
        }

        ItemStack confirmButton = GuiItems.checkmarkButton("&a&lᴄᴏɴꜰɪʀᴍᴇʀ ʟᴀ ᴠᴇɴᴛᴇ");
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        if (confirmMeta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴏʙᴊᴇᴛꜱ ᴠᴇɴᴅᴀʙʟᴇꜱ : &e" + itemCount));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴠᴏᴜꜱ ᴀʟʟᴇᴢ ɢᴀɢɴᴇʀ : &a" + MoneyFormat.format(total) + currency()));
            confirmMeta.setLore(lore);
            confirmButton.setItemMeta(confirmMeta);
        }
        gui.setItem(FishermanSellHolder.CONFIRM_SLOT, confirmButton);
        gui.setItem(FishermanSellHolder.CANCEL_CONFIRM_SLOT, GuiItems.cancelButton("&c&lᴀɴɴᴜʟᴇʀ"));
    }

    public void cancelSellConfirmation(Inventory gui, FishermanSellHolder holder) {
        List<ItemStack> pending = holder.getPendingItems();
        holder.setConfirming(false);
        holder.setPendingItems(null);

        for (int slot : FishermanSellHolder.ITEM_SLOTS) {
            gui.setItem(slot, null);
        }

        if (pending != null) {
            int index = 0;
            for (ItemStack stack : pending) {
                if (index >= FishermanSellHolder.ITEM_SLOTS.length) {
                    break;
                }
                gui.setItem(FishermanSellHolder.ITEM_SLOTS[index], stack);
                index++;
            }
        }

        renderSellNormalState(gui);
    }

    public void confirmSell(Player player, Inventory gui, FishermanSellHolder holder) {
        List<ItemStack> pending = holder.getPendingItems();
        holder.setConfirming(false);
        holder.setPendingItems(null);

        if (pending == null || pending.isEmpty()) {
            player.closeInventory();
            return;
        }

        double total = 0.0;
        int itemsSold = 0;

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
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&aVendu " + itemsSold + " objet(s) au pêcheur pour &e" + MoneyFormat.format(total) + currency() + "&a."));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cAucun objet vendable (seuls le poisson et les cannes à pêche sont acceptés ici)."));
        }

        for (int slot : FishermanSellHolder.ITEM_SLOTS) {
            gui.setItem(slot, null);
        }

        player.closeInventory();
    }

    public void returnSellItems(Player player, Inventory gui, FishermanSellHolder holder) {
        if (holder.isConfirming() && holder.getPendingItems() != null) {
            for (ItemStack stack : holder.getPendingItems()) {
                returnSingleItem(player, stack);
            }
            holder.setPendingItems(null);
            holder.setConfirming(false);
            return;
        }

        for (int slot : FishermanSellHolder.ITEM_SLOTS) {
            ItemStack stack = gui.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            gui.setItem(slot, null);
            returnSingleItem(player, stack);
        }
    }

    private void returnSingleItem(Player player, ItemStack stack) {
        for (ItemStack leftover : player.getInventory().addItem(stack).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    // -------------------------------------------------------------------
    // Achat (catalogue fixe)
    // -------------------------------------------------------------------

    public void openBuy(Player player) {
        FishermanBuyHolder holder = new FishermanBuyHolder();
        Inventory gui = Bukkit.createInventory(holder, FishermanBuyHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', "&6&lᴀᴄʜᴀᴛ ᴀᴜ ᴘᴇᴄʜᴇᴜʀ"));
        holder.setInventory(gui);

        ItemStack filler = GuiItems.nothing(" ");
        for (int i = 0; i < FishermanBuyHolder.SIZE; i++) {
            gui.setItem(i, filler);
        }

        int[] slots = {10, 11, 13, 15, 16};
        for (int i = 0; i < BUY_OFFERS.size(); i++) {
            BuyOffer offer = BUY_OFFERS.get(i);
            int slot = slots[i];
            gui.setItem(slot, buildBuyDisplayItem(offer));
            holder.mapSlot(slot, offer.id());
        }

        gui.setItem(FishermanBuyHolder.BACK_SLOT, GuiItems.cancelButton("&c&l« ʀᴇᴛᴏᴜʀ »"));

        player.openInventory(gui);
    }

    private ItemStack buildBuyDisplayItem(BuyOffer offer) {
        ItemStack item = new ItemStack(offer.material());
        if (offer.enchantment() != null) {
            ItemMeta enchMeta = item.getItemMeta();
            if (enchMeta != null) {
                enchMeta.addEnchant(offer.enchantment(), offer.enchantLevel(), true);
                item.setItemMeta(enchMeta);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', offer.displayName()));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘʀɪx : &a" + MoneyFormat.format(getBuyPrice(offer)) + currency()));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eᴄʟɪǫᴜᴇ ᴘᴏᴜʀ ᴀᴄʜᴇᴛᴇʀ"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Achète 1 exemplaire de l'offre donnée : vérifie le solde, débite, puis
     * donne l'objet (fait tomber au sol le surplus si l'inventaire est plein).
     */
    public void purchase(Player player, String offerId) {
        BuyOffer offer = BUY_OFFERS.stream().filter(o -> o.id().equals(offerId)).findFirst().orElse(null);
        if (offer == null) {
            return;
        }

        double price = getBuyPrice(offer);
        if (!economyManager.has(player.getUniqueId(), price)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cTu n'as pas assez d'argent pour acheter ça."));
            return;
        }

        if (!economyManager.withdraw(player.getUniqueId(), price)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cTu n'as pas assez d'argent pour acheter ça."));
            return;
        }

        ItemStack toGive = new ItemStack(offer.material());
        if (offer.enchantment() != null) {
            ItemMeta meta = toGive.getItemMeta();
            if (meta != null) {
                meta.addEnchant(offer.enchantment(), offer.enchantLevel(), true);
                toGive.setItemMeta(meta);
            }
        }

        for (ItemStack leftover : player.getInventory().addItem(toGive).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&aAcheté pour &e" + MoneyFormat.format(price) + currency() + "&a."));
    }
}
