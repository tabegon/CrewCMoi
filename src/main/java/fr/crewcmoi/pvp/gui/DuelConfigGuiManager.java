package fr.crewcmoi.pvp.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.other.utils.MoneyFormat;
import fr.crewcmoi.pvp.managers.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

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
                color("&8&lDuel Contre " + target.getName()));
        holder.setInventory(gui);
        render(holder);
        requester.openInventory(gui);
    }

    public void render(DuelConfigHolder holder) {
        Inventory gui = holder.getInventory();
        for (int i = 0; i < DuelConfigHolder.SIZE; i++) {
            gui.setItem(i, GuiItems.nothing(" "));
        }

        gui.setItem(0, GuiItems.keepInventory(holder.isKeepInventory(), holder.isKeepInventory()
                ? "&a&lᴋᴇᴇᴘɪɴᴠᴇɴᴛᴏʀʏ ᴏɴ" : "&c&lᴋᴇᴇᴘɪɴᴠᴇɴᴛᴏʀʏ ᴏꜰꜰ"));
        gui.setItem(2, GuiItems.headDrop(holder.isDropHead(),
                holder.isDropHead() ? "&a&lᴘʟᴀʏᴇʀ ʜᴇᴀᴅ ᴅʀᴏᴘ" : "&c&lᴘʟᴀʏᴇʀ ʜᴇᴀᴅ ᴅᴏɴ'ᴛ ᴅʀᴏᴘ"));
        gui.setItem(4, buildBetItem(holder));
        gui.setItem(6, GuiItems.kit(holder.getKitId() != null,
                holder.getKitId() != null ? "&a&lᴀᴠᴇᴄ ᴋɪᴛ" : "&e&lꜱᴀɴꜱ ᴋɪᴛ"));
        gui.setItem(8, GuiItems.elytra(holder.isAllowElytra(),
                holder.isAllowElytra() ? "&a&lᴇʟʏᴛʀᴀ ᴏɴ" : "&c&lᴇʟʏᴛʀᴀ ᴏꜰꜰ"));

        gui.setItem(13, GuiItems.duelButton("&e&lᴅᴜᴇʟ ᴄᴏɴᴛʀᴇ " + holder.getTargetName()));
        gui.setItem(21, GuiItems.cancelButton("&c&lɢᴏ ʙᴀᴄᴋ"));
        gui.setItem(23, GuiItems.checkmarkButton("&a&lᴀꜱᴋ ᴅᴜᴇʟ"));
    }

    private ItemStack buildBetItem(DuelConfigHolder holder) {
        ItemStack item = GuiItems.moneyButton("&e&lᴍɪꜱᴇ : &a" + MoneyFormat.format(holder.getBet()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Mise actuelle : &e" + MoneyFormat.format(holder.getBet())));
            lore.add("");
            lore.add(color("&eClic gauche &7: +" + MoneyFormat.format(duelManager.getBetStep())));
            lore.add(color("&eClic droit &7: -" + MoneyFormat.format(duelManager.getBetStep())));
            lore.add(color("&eClic molette &7: entrer un montant exact"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isKeepInventoryLocked(DuelConfigHolder holder) {
        return holder.getKitId() != null;
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
