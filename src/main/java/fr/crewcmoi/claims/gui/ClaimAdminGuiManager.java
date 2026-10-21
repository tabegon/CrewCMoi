package fr.crewcmoi.claims.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.managers.ClaimManager;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.other.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClaimAdminGuiManager {

    private static final int SIZE = 54;

    /** Points testés dans le chunk (centre d'abord) pour trouver un emplacement sûr. */
    private static final int[][] TELEPORT_OFFSETS = {
            {8, 8}, {4, 4}, {12, 4}, {4, 12}, {12, 12}, {8, 2}, {2, 8}, {14, 8}, {8, 14}
    };
    private static final Set<Material> HAZARDOUS_FLOORS = Set.of(
            Material.MAGMA_BLOCK, Material.CACTUS, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.FIRE, Material.SOUL_FIRE, Material.LAVA, Material.WATER
    );

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
        lore.add(color("&7Clic gauche sur un claim : &fouvrir ses réglages"));
        lore.add(color("&7Clic droit sur un claim : &fvous y téléporter"));
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
            lore.add(color("&eClic gauche : gérer ce claim"));
            lore.add(color("&bClic droit : se téléporter"));
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

    /**
     * Téléporte un administrateur dans un claim existant.
     * Ne lit que les données du ClaimManager et n'écrit rien dans les claims.
     *
     * @return true si la téléportation a été effectuée
     */
    public boolean teleportToClaim(Player admin, String worldName, int chunkX, int chunkZ) {
        if (!admin.hasPermission("crew.claim.admin")) {
            admin.closeInventory();
            return false;
        }

        ClaimData claim = claimManager.getClaim(worldName, chunkX, chunkZ);
        if (claim == null) {
            Messages.send(admin, "claims.admin.teleport-claim-missing");
            open(admin);
            return false;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Messages.send(admin, "claims.admin.teleport-world-missing", Map.of("world", worldName));
            return false;
        }

        Location origin = admin.getLocation();
        Location destination = findSafeLocation(world, chunkX, chunkZ, origin.getYaw(), origin.getPitch());
        boolean safe = destination != null;
        if (!safe) {
            int x = (chunkX << 4) + 8;
            int z = (chunkZ << 4) + 8;
            int y = Math.min(world.getHighestBlockYAt(x, z) + 1, world.getMaxHeight() - 2);
            destination = new Location(world, x + 0.5, y, z + 0.5, origin.getYaw(), origin.getPitch());
        }

        admin.closeInventory();
        if (!admin.teleport(destination)) {
            Messages.send(admin, "claims.admin.teleport-failed");
            return false;
        }

        Map<String, Object> placeholders = Map.of(
                "world", worldName, "x", chunkX, "z", chunkZ, "owner", claim.getOwnerName());
        Messages.send(admin, "claims.admin.teleport-success", placeholders);
        if (!safe) {
            Messages.send(admin, "claims.admin.teleport-unsafe");
        }
        plugin.getLogger().info(admin.getName() + " s'est téléporté au claim " + worldName + ";" + chunkX + ";"
                + chunkZ + " de " + claim.getOwnerName() + ".");
        return true;
    }

    private Location findSafeLocation(World world, int chunkX, int chunkZ, float yaw, float pitch) {
        for (int[] offset : TELEPORT_OFFSETS) {
            int x = (chunkX << 4) + offset[0];
            int z = (chunkZ << 4) + offset[1];
            Integer feetY = findSafeFeetY(world, x, z);
            if (feetY != null) {
                return new Location(world, x + 0.5, feetY, z + 0.5, yaw, pitch);
            }
        }
        return null;
    }

    private Integer findSafeFeetY(World world, int x, int z) {
        boolean nether = world.getEnvironment() == World.Environment.NETHER;
        // Nether : le plafond de bedrock est le bloc le plus haut, on cherche donc une poche d'air sous le toit.
        int top = nether ? Math.min(world.getMaxHeight() - 1, 120) : world.getHighestBlockYAt(x, z);
        int bottom = nether ? world.getMinHeight() + 1 : top;
        for (int floorY = top; floorY >= bottom; floorY--) {
            if (isSafeStandingSpot(world, x, floorY, z)) {
                return floorY + 1;
            }
        }
        return null;
    }

    private boolean isSafeStandingSpot(World world, int x, int floorY, int z) {
        if (floorY + 2 >= world.getMaxHeight()) return false;
        Block floor = world.getBlockAt(x, floorY, z);
        Block feet = world.getBlockAt(x, floorY + 1, z);
        Block head = world.getBlockAt(x, floorY + 2, z);
        return floor.getType().isSolid()
                && !HAZARDOUS_FLOORS.contains(floor.getType())
                && feet.isPassable() && !feet.isLiquid()
                && head.isPassable() && !head.isLiquid();
    }

    public void openSettings(Player player, String world, int chunkX, int chunkZ) {
        claimSettingsGuiManager.openAdmin(player, world, chunkX, chunkZ);
    }
}
