package fr.crewcmoi.economie.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class QuestMasterGuiManager {
    private final Main plugin;

    public QuestMasterGuiManager(Main plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        QuestMasterHolder holder = new QuestMasterHolder();
        Inventory gui = Bukkit.createInventory(holder, 27, Messages.color("&5&lQuest Master"));
        holder.setInventory(gui);

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&d&lVoyage vers le monde Quest"));
            meta.setLore(java.util.List.of(
                    Messages.color("&7J'ai besoin de vous pour retrouver mon ami."),
                    "",
                    Messages.color("&eCliquez pour vous téléporter")
            ));
            item.setItemMeta(meta);
        }
        gui.setItem(13, item);

        player.openInventory(gui);
    }
}
