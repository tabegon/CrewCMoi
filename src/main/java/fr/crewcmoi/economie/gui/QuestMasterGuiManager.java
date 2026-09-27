package fr.crewcmoi.economie.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.other.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class QuestMasterGuiManager {
    private final Main plugin;

    public QuestMasterGuiManager(Main plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        QuestMasterHolder holder = new QuestMasterHolder();
        Inventory gui = Bukkit.createInventory(holder, 27, Messages.color("&5&lQuest Master"));
        holder.setInventory(gui);

        // Toutes les cases réellement vides utilisent NOTHING.
        for (int slot = 0; slot < gui.getSize(); slot++) {
            gui.setItem(slot, GuiItems.nothing(" "));
        }

        // La ligne centrale contient les 7 éléments du Quest Master.
        gui.setItem(10, GuiItems.goat(Messages.color("&a&lGOAT")));
        for (int slot = 11; slot <= 16; slot++) {
            gui.setItem(slot, GuiItems.lock(Messages.color("&c&lVERROUILLÉ")));
        }

        player.openInventory(gui);
    }
}
