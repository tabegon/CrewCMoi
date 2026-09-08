package fr.crewcmoi.pvp.listeners;

import fr.crewcmoi.pvp.managers.DuelArenaManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;

public class DuelArenaListener implements Listener {

    
    private static final String BYPASS_PERMISSION = "crew.duel.arena.bypass";

    private final DuelArenaManager duelArenaManager;

    public DuelArenaListener(DuelArenaManager duelArenaManager) {
        this.duelArenaManager = duelArenaManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }
        if (duelArenaManager.isInsideArena(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        removeArenaBlocks(event.blockList().iterator());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        removeArenaBlocks(event.blockList().iterator());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (duelArenaManager.isInsideArena(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (duelArenaManager.isInsideArena(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    private void removeArenaBlocks(Iterator<Block> blocks) {
        while (blocks.hasNext()) {
            Block block = blocks.next();
            if (duelArenaManager.isInsideArena(block.getLocation())) {
                blocks.remove();
            }
        }
    }
}
