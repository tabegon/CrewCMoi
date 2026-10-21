package fr.crewcmoi.claims.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.database.ClaimFlag;
import fr.crewcmoi.claims.managers.ClaimManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
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
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.Iterator;

public class ClaimListener implements Listener {

    private final Main plugin;
    private final ClaimManager claimManager;

    public ClaimListener(Main plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    private void deny(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix") +
                        plugin.getMessages().getString("claim.protected")));
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
        
        boolean container = type.contains("CHEST") || type.contains("FURNACE") || type.contains("BARREL")
                || type.contains("SHULKER") || type.contains("ANVIL") || type.contains("ENCHANT")
                || type.contains("HOPPER") || type.contains("DISPENSER") || type.contains("DROPPER")
                || type.contains("BREWING") || type.contains("CRAFTING");
        
        boolean interact = type.contains("DOOR") || type.contains("TRAPDOOR") || type.contains("GATE")
                || type.contains("BUTTON") || type.contains("LEVER") || type.contains("BED")
                || type.contains("REPEATER") || type.contains("COMPARATOR") || type.contains("NOTE_BLOCK")
                || type.contains("CAULDRON");

        ClaimFlag flag = container ? ClaimFlag.CONTAINERS : (interact ? ClaimFlag.INTERACT : null);

        
        
        if (flag == ClaimFlag.INTERACT
                && !claimManager.isAllowed(event.getPlayer(), event.getClickedBlock().getLocation(), flag)) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (top == null) {
            return;
        }

        if (top.getType() == org.bukkit.event.inventory.InventoryType.ENDER_CHEST) {
            if (isProtectedStorageItem(event.getCursor()) && isPlacingIntoTop(event)) {
                event.setCancelled(true);
                denyProtectedStorageItem(player);
                return;
            }

            if (event.getClickedInventory() == top
                    && event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                    && event.getHotbarButton() >= 0 && event.getHotbarButton() < 9) {
                ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                if (isProtectedStorageItem(hotbarItem) || isBundle(hotbarItem)) {
                    event.setCancelled(true);
                    denyProtectedStorageItem(player);
                    return;
                }
            }

            if (event.getClickedInventory() == top
                    && event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) {
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (isProtectedStorageItem(offhand) || isBundle(offhand)) {
                    event.setCancelled(true);
                    denyProtectedStorageItem(player);
                    return;
                }
            }

            if (event.getClickedInventory() == top && isContainerExtraction(event, top)
                    && isBundle(event.getCurrentItem())) {
                event.setCancelled(true);
                denyProtectedStorageItem(player);
                return;
            }

            if (event.getClickedInventory() != top
                    && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    && (isProtectedStorageItem(event.getCurrentItem()) || isBundle(event.getCurrentItem()))) {
                event.setCancelled(true);
                denyProtectedStorageItem(player);
                return;
            }

            if (isBundle(event.getCursor()) && isPlacingIntoTop(event)) {
                event.setCancelled(true);
                denyProtectedStorageItem(player);
            }
            return;
        }

        Location containerLocation = getInventoryLocation(top);
        if (containerLocation == null) {
            return;
        }

        boolean protectedClaim = isProtectedClaimInventory(player, top);

        // Les objets rares restent protégés même si le coffre autorise les conteneurs publics.
        if (protectedClaim && isProtectedClaimItemBeingExtracted(event, top)) {
            event.setCancelled(true);
            denyClaimProtectedItem(player);
            return;
        }

        boolean containerAllowed = claimManager.isAllowed(player, containerLocation, ClaimFlag.CONTAINERS);
        if ((protectedClaim || !containerAllowed) && isBundleTransferToContainer(event, top, player)) {
            event.setCancelled(true);
            denyContainerLoot(player);
            return;
        }

        if (containerAllowed) {
            return;
        }

        if (!isContainerExtraction(event, top)) {
            return;
        }

        ItemStack item = getExtractedItem(event, top);
        if (item == null) {
            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                    && containsNonStealableMatchingItem(top, event.getCursor())) {
                event.setCancelled(true);
                denyContainerLoot(player);
            }
            return;
        }

        if (isProtectedFromClaimTheft(item) || !isStealable(item)) {
            event.setCancelled(true);
            denyContainerLoot(player);
        }
    }

    private Location getInventoryLocation(Inventory inventory) {
        Location location = inventory.getLocation();
        if (location != null) {
            return location;
        }
        return getHolderLocation(inventory.getHolder());
    }

    private Location getHolderLocation(InventoryHolder holder) {
        if (holder == null) {
            return null;
        }
        if (holder instanceof BlockState blockState) {
            return blockState.getLocation();
        }
        if (holder instanceof DoubleChest doubleChest) {
            return doubleChest.getLocation();
        }
        return holder.getInventory() == null ? null : holder.getInventory().getLocation();
    }

    private boolean isProtectedClaimInventory(Player player, Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof DoubleChest doubleChest) {
            return isProtectedClaimLocation(player, getHolderLocation(doubleChest.getLeftSide()))
                    || isProtectedClaimLocation(player, getHolderLocation(doubleChest.getRightSide()));
        }
        return isProtectedClaimLocation(player, getInventoryLocation(inventory));
    }

    private boolean isProtectedClaimLocation(Player player, Location location) {
        ClaimData claim = claimManager.getClaim(location);
        return claim != null && !claimManager.canBuild(player, location);
    }

    private boolean isInventoryInAnyClaim(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof DoubleChest doubleChest) {
            return claimManager.getClaim(getHolderLocation(doubleChest.getLeftSide())) != null
                    || claimManager.getClaim(getHolderLocation(doubleChest.getRightSide())) != null;
        }
        return claimManager.getClaim(getInventoryLocation(inventory)) != null;
    }

    private boolean isProtectedClaimItemBeingExtracted(InventoryClickEvent event, Inventory top) {
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                return false;
            }
            for (ItemStack item : top.getContents()) {
                if (item != null && isProtectedFromClaimTheft(item) && item.isSimilar(cursor)) {
                    return true;
                }
            }
            return false;
        }

        if (event.getClickedInventory() != top || !isContainerExtraction(event, top)) {
            return false;
        }
        return isProtectedFromClaimTheft(event.getCurrentItem());
    }

    private boolean isBundleTransferToContainer(InventoryClickEvent event, Inventory top, Player player) {
        Inventory clicked = event.getClickedInventory();
        if (clicked == top) {
            if (isBundle(event.getCurrentItem()) && isContainerExtraction(event, top)) {
                return true;
            }
            if (isBundle(event.getCursor()) && isPlacingIntoTop(event)) {
                return true;
            }
            if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                    && event.getHotbarButton() >= 0 && event.getHotbarButton() < 9
                    && isBundle(player.getInventory().getItem(event.getHotbarButton()))) {
                return true;
            }
            if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND
                    && isBundle(player.getInventory().getItemInOffHand())) {
                return true;
            }
        }

        return clicked == event.getView().getBottomInventory()
                && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && isBundle(event.getCurrentItem());
    }

    private boolean isPlacingIntoTop(InventoryClickEvent event) {
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            return switch (event.getAction()) {
                case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> true;
                default -> false;
            };
        }
        return event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && event.getClickedInventory() == event.getView().getBottomInventory();
    }

    private boolean isProtectedStorageItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return switch (item.getType()) {
            case HEAVY_CORE, MACE, DRAGON_EGG, ELYTRA, PLAYER_HEAD, PLAYER_WALL_HEAD,
                    DRAGON_HEAD, DRAGON_WALL_HEAD -> true;
            default -> false;
        };
    }

    private boolean isProtectedFromClaimTheft(ItemStack item) {
        return isProtectedStorageItem(item);
    }

    private void denyProtectedStorageItem(Player player) {
        player.sendMessage(ChatColor.RED + "Cet objet ne peut pas être placé dans l'Ender Chest.");
    }

    private boolean isContainerExtraction(InventoryClickEvent event, Inventory top) {
        Inventory clicked = event.getClickedInventory();
        InventoryAction action = event.getAction();

        
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

    private boolean containsNonStealableMatchingItem(Inventory inventory, ItemStack cursor) {
        if (cursor == null || cursor.getType() == Material.AIR) {
            return false;
        }

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (item.isSimilar(cursor) && (isProtectedFromClaimTheft(item) || !isStealable(item))) {
                return true;
            }
        }
        return false;
    }

    private boolean isStealable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || isProtectedFromClaimTheft(item)) {
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

    private void denyClaimProtectedItem(Player player) {
        player.sendMessage(ChatColor.RED + "Cet objet ne peut pas être volé dans un coffre de claim.");
    }

    private void denyContainerLoot(Player player) {
        String message = plugin.getMessages().getString(
                "claim.container-item-blocked",
                "&cVous ne pouvez pas voler cet objet dans ce conteneur."
        );
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix") + message));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBundleUseWhileContainerOpen(PlayerInteractEvent event) {
        if (!isBundle(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top == null) {
            return;
        }

        if (top.getType() == org.bukkit.event.inventory.InventoryType.ENDER_CHEST) {
            event.setCancelled(true);
            denyProtectedStorageItem(player);
            return;
        }

        Location location = getInventoryLocation(top);
        if (location == null) {
            return;
        }

        if (isProtectedClaimInventory(player, top)
                || !claimManager.isAllowed(player, location, ClaimFlag.CONTAINERS)) {
            event.setCancelled(true);
            denyContainerLoot(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onProtectedItemMovedByInventory(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (!isProtectedFromClaimTheft(item) && !isBundle(item)) {
            return;
        }

        Location sourceLocation = getInventoryLocation(event.getSource());
        if (sourceLocation == null) {
            return;
        }

        if (isInventoryInAnyClaim(event.getSource())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;

        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize());
        if (!touchesTop) return;

        if (top.getType() == org.bukkit.event.inventory.InventoryType.ENDER_CHEST) {
            if (isProtectedStorageItem(event.getOldCursor()) || isBundle(event.getOldCursor())) {
                event.setCancelled(true);
                denyProtectedStorageItem(player);
            }
            return;
        }

        Location location = getInventoryLocation(top);
        if (location == null) return;
        boolean protectedClaim = isProtectedClaimInventory(player, top);
        boolean containerAllowed = claimManager.isAllowed(player, location, ClaimFlag.CONTAINERS);
        if (isBundle(event.getOldCursor()) && (protectedClaim || !containerAllowed)) {
            event.setCancelled(true);
            denyContainerLoot(player);
        }
    }

    private boolean isBundle(ItemStack item) {
        return item != null && item.getType() == Material.BUNDLE;
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

        Entity entity = event.getEntity();
        if (!(entity instanceof Player) && !claimManager.isOpen(event.getBlock().getLocation(), ClaimFlag.MOB_GRIEFING)) {
            event.setCancelled(true);
        }
    }
}
