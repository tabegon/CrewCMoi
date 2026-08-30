package fr.crewcmoi.claims.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.database.ClaimFlag;
import fr.crewcmoi.claims.database.ClaimPermission;
import fr.crewcmoi.claims.managers.ClaimManager;
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
 * Construit et rafraîchit la GUI /claims settings : permet au propriétaire d'un claim
 * de choisir, pour chaque règle (construire, détruire, feu, explosions...), qui est
 * autorisé à faire l'action (propriétaire uniquement / joueurs de confiance / tout le monde).
 */
public class ClaimSettingsGuiManager {

    private static final int SIZE = 27;

    private final Main plugin;
    private final ClaimManager claimManager;

    public ClaimSettingsGuiManager(Main plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    public void open(Player player, String world, int chunkX, int chunkZ) {
        ClaimSettingsHolder holder = new ClaimSettingsHolder(world, chunkX, chunkZ);
        Inventory gui = Bukkit.createInventory(holder, SIZE,
                ChatColor.translateAlternateColorCodes('&', "&8&lRègles du claim"));
        holder.setInventory(gui);
        render(holder);
        player.openInventory(gui);
    }

    public void render(ClaimSettingsHolder holder) {
        Inventory gui = holder.getInventory();
        for (int i = 0; i < SIZE; i++) {
            gui.setItem(i, null);
        }

        ClaimData claim = claimManager.getClaim(holder.getWorld(), holder.getChunkX(), holder.getChunkZ());
        if (claim == null) {
            return;
        }

        ClaimFlag[] flags = ClaimFlag.values();
        int slot = 10;
        for (ClaimFlag flag : flags) {
            // Place les règles sur la 2e et 3e ligne du coffre (slots 10-16), en sautant
            // les bordures pour un rendu plus propre.
            if (slot % 9 == 8) {
                slot += 3;
            }
            gui.setItem(slot, buildItem(flag, claim.getPermission(flag)));
            holder.mapSlot(slot, flag);
            slot++;
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lInfo"));
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.translateAlternateColorCodes('&', "&7Cliquez sur une règle pour changer"));
        infoLore.add(ChatColor.translateAlternateColorCodes('&', "&7qui est autorisé à l'effectuer."));
        infoLore.add("");
        infoLore.add(ChatColor.translateAlternateColorCodes('&', "&7Le propriétaire peut toujours tout faire."));
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        gui.setItem(4, info);
    }

    private ItemStack buildItem(ClaimFlag flag, ClaimPermission permission) {
        ItemStack item = new ItemStack(materialFor(permission));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&l" + flag.getDisplayName()));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7" + flag.getDescription()));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Autorisé pour : " + colorFor(permission)
                + permission.getDisplayName()));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eClic gauche pour changer"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material materialFor(ClaimPermission permission) {
        return switch (permission) {
            case OWNER_ONLY -> Material.RED_WOOL;
            case TRUSTED -> Material.YELLOW_WOOL;
            case EVERYONE -> Material.LIME_WOOL;
        };
    }

    private String colorFor(ClaimPermission permission) {
        return switch (permission) {
            case OWNER_ONLY -> "&c";
            case TRUSTED -> "&e";
            case EVERYONE -> "&a";
        };
    }
}
