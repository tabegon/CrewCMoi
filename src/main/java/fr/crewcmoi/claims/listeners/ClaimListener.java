package fr.crewcmoi.claims.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.database.ClaimFlag;
import fr.crewcmoi.claims.managers.ClaimManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.Iterator;

/**
 * Protège les chunks claim : personne d'autre que le propriétaire (ou un joueur de
 * confiance) ne peut y construire, détruire, mettre le feu, faire exploser, utiliser
 * un seau, ouvrir des coffres/portes, etc.
 */
public class ClaimListener implements Listener {

    private final Main plugin;
    private final ClaimManager claimManager;

    public ClaimListener(Main plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    private void deny(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix", "") +
                        plugin.getMessages().getString("claim.protected", "&cCe chunk est protégé par un claim, vous ne pouvez pas faire cela ici.")));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!claimManager.isAllowed(event.getPlayer(), event.getBlock().getLocation(), ClaimFlag.BREAK)) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!claimManager.isAllowed(event.getPlayer(), event.getBlock().getLocation(), ClaimFlag.BUILD)) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        String type = event.getClickedBlock().getType().name();
        // Règle CONTAINERS : coffres, fours, tonneaux, enclumes, tables d'enchantement, etc.
        boolean container = type.contains("CHEST") || type.contains("FURNACE") || type.contains("BARREL")
                || type.contains("SHULKER") || type.contains("ANVIL") || type.contains("ENCHANT")
                || type.contains("HOPPER") || type.contains("DISPENSER") || type.contains("DROPPER")
                || type.contains("BREWING") || type.contains("CRAFTING");
        // Règle INTERACT : portes, leviers, boutons, lits, etc.
        boolean interact = type.contains("DOOR") || type.contains("TRAPDOOR") || type.contains("GATE")
                || type.contains("BUTTON") || type.contains("LEVER") || type.contains("BED")
                || type.contains("REPEATER") || type.contains("COMPARATOR") || type.contains("NOTE_BLOCK")
                || type.contains("CAULDRON");

        ClaimFlag flag = container ? ClaimFlag.CONTAINERS : (interact ? ClaimFlag.INTERACT : null);

        // Important : un claim avec CONTAINERS désactivé, on laisse quand même le joueur
        // ouvrir le coffre/four/tonneau/etc. La protection du contenu est appliquée dans
        // onInventoryClick() ci-dessous.
        if (flag == ClaimFlag.INTERACT
                && !claimManager.isAllowed(event.getPlayer(), event.getClickedBlock().getLocation(), flag)) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    /**
     * Protège le contenu des conteneurs d'un claim.
     *
     * Le joueur peut ouvrir le conteneur même si CONTAINERS lui est interdit, mais il ne
     * peut pas en retirer les objets. Les objets dont le Material figure dans
     * claims.stealable-items restent récupérables.
     *
     * Le propriétaire, les joueurs de confiance et les joueurs ayant crew.claim.bypass
     * conservent le comportement normal via ClaimManager.isAllowed().
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (top == null || top.getLocation() == null) {
            return;
        }

        Location containerLocation = top.getLocation();

        // Hors claim ou joueur autorisé : comportement normal.
        if (claimManager.isAllowed(player, containerLocation, ClaimFlag.CONTAINERS)) {
            return;
        }

        // Seules les actions qui peuvent retirer un objet du conteneur sont bloquées.
        // Placer des objets dans le conteneur reste autorisé.
        if (!isContainerExtraction(event, top)) {
            return;
        }

        // Pour les clics directs, on peut vérifier précisément l'objet concerné.
        ItemStack item = getExtractedItem(event, top);

        // Certaines actions (notamment COLLECT_TO_CURSOR) peuvent toucher plusieurs
        // emplacements. Dans ce cas, on autorise seulement si aucun objet non autorisé
        // ne pourrait être récupéré.
        if (item == null) {
            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                if (containsNonStealableMatchingItem(top, event.getCursor())) {
                    event.setCancelled(true);
                    denyContainerLoot(player);
                }
            }
            return;
        }

        if (!isStealable(item)) {
            event.setCancelled(true);
            denyContainerLoot(player);
        }
    }

    /**
     * Retourne true si l'action retire potentiellement un objet de l'inventaire supérieur.
     */
    private boolean isContainerExtraction(InventoryClickEvent event, Inventory top) {
        Inventory clicked = event.getClickedInventory();
        InventoryAction action = event.getAction();

        // Un double-clic peut collecter des objets depuis l'inventaire supérieur même si
        // le slot actuellement cliqué se trouve dans l'inventaire du joueur.
        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            return true;
        }

        if (clicked != top) {
            return false;
        }

        return switch (action) {
            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE,
                 SWAP_WITH_CURSOR, MOVE_TO_OTHER_INVENTORY,
                 HOTBAR_SWAP, HOTBAR_MOVE_AND_READD,
                 DROP_ALL_SLOT, DROP_ONE_SLOT, CLONE_STACK -> true;
            default -> false;
        };
    }

    /**
     * Récupère l'objet qui serait retiré par une action de clic simple.
     */
    private ItemStack getExtractedItem(InventoryClickEvent event, Inventory top) {
        Inventory clicked = event.getClickedInventory();
        if (clicked != top) {
            return null;
        }

        int slot = event.getSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return null;
        }

        return top.getItem(slot);
    }

    /**
     * Pour un double-clic/collecte, vérifie s'il existe dans le conteneur un objet
     * correspondant au curseur qui n'est pas autorisé au vol.
     */
    private boolean containsNonStealableMatchingItem(Inventory inventory, ItemStack cursor) {
        if (cursor == null || cursor.getType() == Material.AIR) {
            return false;
        }

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (item.isSimilar(cursor) && !isStealable(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Liste configurable dans config.yml :
     * claims:
     *   stealable-items:
     *     - MACE
     *     - DRAGON_EGG
     *     - ELYTRA
     */
    private boolean isStealable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        for (String configured : plugin.getConfig().getStringList("claims.stealable-items")) {
            if (configured == null || configured.isBlank()) {
                continue;
            }

            Material material = Material.matchMaterial(configured.trim());
            if (material == item.getType()) {
                return true;
            }
        }

        return false;
    }

    private void denyContainerLoot(Player player) {
        String message = plugin.getMessages().getString(
                "claim.container-item-blocked",
                "&cVous ne pouvez pas voler cet objet dans ce conteneur."
        );
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix", "") + message));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) { 
        // Un drag part toujours du curseur du joueur vers le menu. Il ne permet pas de
        // retirer un objet déjà présent dans le conteneur : le placement reste donc autorisé.
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        if (player != null) {
            if (!claimManager.isAllowed(player, loc, ClaimFlag.FIRE)) {
                event.setCancelled(true);
                deny(player);
            }
        } else if (!claimManager.isOpen(loc, ClaimFlag.FIRE)) {
            // Propagation de feu non initiée par un joueur (foudre, propagation naturelle) :
            // on bloque quand même si la règle FIRE n'est pas ouverte à tout le monde.
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!claimManager.isOpen(event.getBlock().getLocation(), ClaimFlag.FIRE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!claimManager.isAllowed(event.getPlayer(), event.getBlock().getLocation(), ClaimFlag.BUCKETS)) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (!claimManager.isAllowed(event.getPlayer(), event.getBlock().getLocation(), ClaimFlag.BUCKETS)) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            if (!claimManager.isAllowed(player, event.getEntity().getLocation(), ClaimFlag.BREAK)) {
                event.setCancelled(true);
                deny(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (isPistonBlockedByClaim(event.getBlock().getLocation(), event.getBlocks().stream()
                .map(Block::getLocation).iterator())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (isPistonBlockedByClaim(event.getBlock().getLocation(), event.getBlocks().stream()
                .map(Block::getLocation).iterator())) {
            event.setCancelled(true);
        }
    }

    private boolean isPistonBlockedByClaim(Location pistonLocation, Iterator<Location> movedBlocks) {
        // Empêche un piston situé hors d'un claim de pousser/tirer des blocs à l'intérieur d'un
        // claim (ou inversement), pour éviter de contourner la protection.
        ClaimData pistonClaim = claimManager.getClaim(pistonLocation);
        while (movedBlocks.hasNext()) {
            Location loc = movedBlocks.next();
            ClaimData blockClaim = claimManager.getClaim(loc);
            if (blockClaim != null && blockClaim != pistonClaim) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> !claimManager.isOpen(block.getLocation(), ClaimFlag.EXPLOSIONS));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> !claimManager.isOpen(block.getLocation(), ClaimFlag.EXPLOSIONS));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        // Empêche les mobs (endermen qui déplacent des blocs, sangliers/creepers, etc.) et
        // autres entités non-joueurs de modifier des blocs à l'intérieur d'un claim.
        Entity entity = event.getEntity();
        if (!(entity instanceof Player) && !claimManager.isOpen(event.getBlock().getLocation(), ClaimFlag.MOB_GRIEFING)) {
            event.setCancelled(true);
        }
    }
}
