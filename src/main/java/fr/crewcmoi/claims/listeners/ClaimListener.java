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
        if (flag != null && !claimManager.isAllowed(event.getPlayer(), event.getClickedBlock().getLocation(), flag)) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
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
