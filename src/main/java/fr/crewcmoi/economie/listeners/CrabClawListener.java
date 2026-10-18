package fr.crewcmoi.economie.listeners;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

public class CrabClawListener implements Listener {
    private static final String CLAW_ID = "server:crab_claw";
    private static final String COOKED_ID = "server:cooked_crab_claw";
    private static final String RAW_VENISON_ID = "server:raw_venison";
    private static final String COOKED_VENISON_ID = "server:cooked_venison";
    private static final int MAX_DURABILITY = 128;
    private final Main plugin;
    private final NamespacedKey reachKey;
    private final NamespacedKey durabilityKey;
    private final BukkitTask refreshTask;

    public CrabClawListener(Main plugin) {
        this.plugin = plugin;
        this.reachKey = new NamespacedKey(plugin, "crab_claw_reach");
        this.durabilityKey = new NamespacedKey(plugin, "crab_claw_durability");
        this.refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) updateReach(player);
        }, 1L, 10L);
    }

    public void shutdown() { refreshTask.cancel(); }

    @EventHandler public void onJoin(PlayerJoinEvent event) { Bukkit.getScheduler().runTask(plugin, () -> updateReach(event.getPlayer())); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { removeReach(event.getPlayer()); }
    @EventHandler public void onHeld(PlayerItemHeldEvent event) { Bukkit.getScheduler().runTask(plugin, () -> updateReach(event.getPlayer())); }
    @EventHandler public void onSwap(PlayerSwapHandItemsEvent event) { Bukkit.getScheduler().runTask(plugin, () -> updateReach(event.getPlayer())); }
    @EventHandler public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) Bukkit.getScheduler().runTask(plugin, () -> updateReach(player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isClaw(mainHand)) damageClaw(player, EquipmentSlot.HAND, mainHand);
        else if (isClaw(offHand)) damageClaw(player, EquipmentSlot.OFF_HAND, offHand);
        else return;
        Bukkit.getScheduler().runTask(plugin, () -> updateReach(player));
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        if (isCustomItem(event.getSource(), RAW_VENISON_ID)) {
            setCookedResult(event, COOKED_VENISON_ID, RAW_VENISON_ID);
            return;
        }
        if (isClaw(event.getSource())) {
            setCookedResult(event, COOKED_ID, CLAW_ID);
        }
    }

    private void setCookedResult(FurnaceSmeltEvent event, String cookedId, String rawId) {
        CustomStack cooked = CustomStack.getInstance(cookedId);
        if (cooked == null || cooked.getItemStack() == null) {
            plugin.getLogger().warning("Impossible de cuire " + rawId + " : " + cookedId + " est introuvable dans ItemsAdder.");
            return;
        }
        event.setResult(cooked.getItemStack().clone());
    }

    private boolean isCustomItem(ItemStack item, String id) {
        if (item == null || item.getType().isAir()) return false;
        try {
            CustomStack custom = CustomStack.byItemStack(item);
            return custom != null && id.equalsIgnoreCase(custom.getNamespacedID());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void damageClaw(Player player, EquipmentSlot hand, ItemStack item) {
        ItemStack damagedItem = item.clone();
        ItemStack remaining = null;
        if (item.getAmount() > 1) {
            remaining = item.clone();
            remaining.setAmount(item.getAmount() - 1);
            damagedItem.setAmount(1);
        }
        ItemMeta meta = damagedItem.getItemMeta();
        if (meta == null) return;
        int currentDamage = meta.getPersistentDataContainer().getOrDefault(durabilityKey, PersistentDataType.INTEGER, 0);
        int nextDamage = currentDamage + 1;
        if (nextDamage >= MAX_DURABILITY) {
            if (hand == EquipmentSlot.OFF_HAND) player.getInventory().setItemInOffHand(remaining);
            else player.getInventory().setItemInMainHand(remaining);
            player.sendMessage(ChatColor.RED + "Ta pince de crabe s'est cassée.");
        } else {
            meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, nextDamage);
            damagedItem.setItemMeta(meta);
            if (hand == EquipmentSlot.OFF_HAND) player.getInventory().setItemInOffHand(damagedItem);
            else player.getInventory().setItemInMainHand(damagedItem);
            if (remaining != null) {
                for (ItemStack leftover : player.getInventory().addItem(remaining).values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }
    }

    private boolean isClaw(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        try {
            CustomStack custom = CustomStack.byItemStack(item);
            return custom != null && CLAW_ID.equalsIgnoreCase(custom.getNamespacedID());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void updateReach(Player player) {
        AttributeInstance attribute = getBlockInteractionRange(player);
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(reachKey);
        boolean hasClaw = isClaw(player.getInventory().getItemInMainHand()) || isClaw(player.getInventory().getItemInOffHand());
        if (hasClaw && current == null) {
            attribute.addModifier(new AttributeModifier(reachKey, 1.0, AttributeModifier.Operation.ADD_NUMBER));
        } else if (!hasClaw && current != null) {
            attribute.removeModifier(reachKey);
        }
    }

    private AttributeInstance getBlockInteractionRange(Player player) {
        Attribute attribute = null;
        for (String name : new String[]{"BLOCK_INTERACTION_RANGE", "PLAYER_BLOCK_INTERACTION_RANGE"}) {
            try {
                attribute = Attribute.valueOf(name);
                break;
            } catch (IllegalArgumentException ignored) {
            }
        }
        return attribute == null ? null : player.getAttribute(attribute);
    }

    private void removeReach(Player player) {
        AttributeInstance attribute = getBlockInteractionRange(player);
        if (attribute != null && attribute.getModifier(reachKey) != null) attribute.removeModifier(reachKey);
    }
}
