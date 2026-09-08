package fr.crewcmoi.claims.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.managers.ClaimManager;
import fr.crewcmoi.other.utils.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClaimAdminGuiManager {

    private static final int SIZE = 54;
    private final Main plugin;
    private final ClaimManager claimManager;
    private final ClaimSettingsGuiManager claimSettingsGuiManager;

    public ClaimAdminGuiManager(Main plugin, ClaimManager claimManager,
                                ClaimSettingsGuiManager claimSettingsGuiManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.claimSettingsGuiManager = claimSettingsGuiManager;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<ClaimData> claims = sortedClaims();
        int maxPage = Math.max(0, (claims.size() - 1) / ClaimAdminHolder.ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, maxPage));

        ClaimAdminHolder holder = new ClaimAdminHolder();
        holder.setPage(page);
        Inventory gui = Bukkit.createInventory(holder, SIZE,
                ChatColor.translateAlternateColorCodes('&', "&8&lGestion des claims &7• &f" + (page + 1)));
        holder.setInventory(gui);
        render(holder, claims);
        player.openInventory(gui);
    }

    public void render(ClaimAdminHolder holder, List<ClaimData> claims) {
        Inventory gui = holder.getInventory();
        for (int i = 0; i < SIZE; i++) {
            gui.setItem(i, null);
        }
        holder.clearMapping();

        int start = holder.getPage() * ClaimAdminHolder.ITEMS_PER_PAGE;
        int end = Math.min(start + ClaimAdminHolder.ITEMS_PER_PAGE, claims.size());
        for (int i = start; i < end; i++) {
            ClaimData claim = claims.get(i);
            int slot = i - start;
            gui.setItem(slot, buildClaimItem(claim));
            holder.mapSlot(slot, claim.getWorld(), claim.getChunkX(), claim.getChunkZ());
        }

        gui.setItem(ClaimAdminHolder.PREV_PAGE_SLOT,
                holder.getPage() > 0 ? button(Material.ARROW, "&ePage précédente") : GuiItems.nothing(" "));
        gui.setItem(ClaimAdminHolder.NEXT_PAGE_SLOT,
                end < claims.size() ? button(Material.ARROW, "&ePage suivante") : GuiItems.nothing(" "));

        ItemStack info = button(Material.BOOK, "&e&lGestion des claims");
        ItemMeta meta = info.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Claims totaux : &f" + claims.size()));
        lore.add("");
        lore.add(color("&7Cliquez sur un claim pour ouvrir ses réglages."));
        lore.add(color("&cLa suppression est définitive."));
        meta.setLore(lore);
        info.setItemMeta(meta);
        gui.setItem(ClaimAdminHolder.INFO_SLOT, info);

        ItemStack filler = GuiItems.nothing(" ");
        for (int i = 0; i < SIZE; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }
    }

    private List<ClaimData> sortedClaims() {
        List<ClaimData> claims = claimManager.getAllClaims();
        claims.sort(Comparator.comparing(ClaimData::getWorld, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(ClaimData::getChunkX)
                .thenComparingInt(ClaimData::getChunkZ)
                .thenComparing(ClaimData::getOwnerName, String.CASE_INSENSITIVE_ORDER));
        return claims;
    }

    private ItemStack buildClaimItem(ClaimData claim) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(claim.getOwnerUuid()));
            meta.setDisplayName(color("&e&lClaim &7#" + claim.getChunkX() + "," + claim.getChunkZ()));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Propriétaire : &f" + claim.getOwnerName()));
            lore.add(color("&7Monde : &f" + claim.getWorld()));
            lore.add(color("&7Chunk : &f" + claim.getChunkX() + " &7/ &f" + claim.getChunkZ()));
            lore.add(color("&7Joueurs de confiance : &f" + claim.getTrusted().size()));
            lore.add(color("&7En vente : " + (claim.isForSale() ? "&aoui" : "&cnon")));
            lore.add("");
            lore.add(color("&eCliquez pour gérer ce claim"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public void openSettings(Player player, String world, int chunkX, int chunkZ) {
        claimSettingsGuiManager.openAdmin(player, world, chunkX, chunkZ);
    }
}
