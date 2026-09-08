package fr.crewcmoi.pets;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PetGuiManager {
    public static final String TITLE = "";
    public static final int SIZE = 54;

    private final Main plugin;
    private final PetManager petManager;

    public PetGuiManager(Main plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;
    }

    public void open(org.bukkit.entity.Player player) {
        Inventory gui = Bukkit.createInventory(null, SIZE, Messages.color(plugin.getMessages().getString("gui.pets.title")));
        int index = 0;
        for (String petId : petManager.getConfiguredPets()) {
            if (index >= SIZE) break;
            ItemStack item = petManager.createPetItem(petId);
            if (item == null) {
                plugin.getLogger().warning("Pet '" + petId + "' : impossible de charger l'item ItemsAdder.");
                continue;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                boolean owned = petManager.owns(player.getUniqueId(), petId);
                boolean active = petId.equalsIgnoreCase(petManager.getActivePet(player.getUniqueId()));
                lore.add(owned
                        ? (active ? Messages.color(plugin.getMessages().getString("gui.pets.active")) : Messages.color(plugin.getMessages().getString("gui.pets.activate")))
                        : Messages.color(plugin.getMessages().getString("gui.pets.locked")));
                if (owned) lore.add(Messages.color(plugin.getMessages().getString("gui.pets.deactivate")));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            gui.setItem(petManager.getSlot(petId, index), item);
            index++;
        }

        player.openInventory(gui);
    }
}
