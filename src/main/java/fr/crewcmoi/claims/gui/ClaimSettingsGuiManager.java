package fr.crewcmoi.claims.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.database.ClaimFlag;
import fr.crewcmoi.claims.database.ClaimPermission;
import fr.crewcmoi.claims.managers.ClaimManager;
import fr.crewcmoi.other.utils.GuiItems;
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
    private static final int MOB_GRIEFING_SLOT = 22;

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

        // MOB_GRIEFING est affiché à part, centré au slot 23 (2e slot de la 3e ligne
        // en partant du centre) ; les autres règles se partagent la 2e ligne (10-16).
        int slot = 10;
        for (ClaimFlag flag : ClaimFlag.values()) {
            if (flag == ClaimFlag.MOB_GRIEFING) {
                continue;
            }
            gui.setItem(slot, buildItem(flag, claim.getPermission(flag)));
            holder.mapSlot(slot, flag);
            slot++;
        }

        gui.setItem(MOB_GRIEFING_SLOT, buildItem(ClaimFlag.MOB_GRIEFING, claim.getPermission(ClaimFlag.MOB_GRIEFING)));
        holder.mapSlot(MOB_GRIEFING_SLOT, ClaimFlag.MOB_GRIEFING);

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

        // Remplit tous les emplacements encore vides (bordures, trous entre les règles...)
        // avec l'item de remplissage neutre, pour ne plus laisser de slots visuellement vides.
        ItemStack filler = GuiItems.nothing(" ");
        for (int i = 0; i < SIZE; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }

    private ItemStack buildItem(ClaimFlag flag, ClaimPermission permission) {
        ItemStack item = new ItemStack(materialFor(flag));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', colorFor(permission) + "&l" + flag.getDisplayName()));

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

    /**
     * Icône représentant le type de règle (et non plus la permission actuelle,
     * qui laine indiquait auparavant via sa couleur). La permission actuelle est
     * désormais indiquée par la couleur du nom de l'item et la ligne de lore
     * "Autorisé pour : ...".
     */
    private Material materialFor(ClaimFlag flag) {
        return switch (flag) {
            case BUILD -> Material.BRICKS;
            case BREAK -> Material.IRON_PICKAXE;
            case CONTAINERS -> Material.CHEST;
            case INTERACT -> Material.LEVER;
            case BUCKETS -> Material.WATER_BUCKET;
            case FIRE -> Material.FLINT_AND_STEEL;
            case EXPLOSIONS -> Material.TNT;
            case MOB_GRIEFING -> Material.ZOMBIE_HEAD;
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
