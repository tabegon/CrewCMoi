package fr.crewcmoi.economie.listeners;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurniturePlaceSuccessEvent;
import fr.crewcmoi.Main;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.other.utils.MoneyFormat;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MoneyBagListener implements Listener {
    private static final String DEFAULT_BAG_ID = "server:ccoin_bag";
    private final Main plugin;
    private final EconomyManager economy;
    private final NamespacedKey valueKey;
    private final NamespacedKey placedBagKey;
    private final NamespacedKey creativeBagKey;
    private final Map<UUID, PendingBag> pendingPlacements = new ConcurrentHashMap<>();

    public MoneyBagListener(Main plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.valueKey = new NamespacedKey(plugin, "money_bag_value");
        this.placedBagKey = new NamespacedKey(plugin, "placed_money_bag_value");
        this.creativeBagKey = new NamespacedKey(plugin, "creative_money_bag");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onUseBag(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isBag(held)) return;

        Double value = readValue(held);
        boolean creative = player.getGameMode() == GameMode.CREATIVE;

        if (player.isSneaking()) {
            event.setCancelled(true);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            if (value == null || !Double.isFinite(value) || value <= 0) {
                player.sendMessage(ChatColor.RED + "Ce sac ne contient aucune valeur à empocher.");
                return;
            }
            consumeOne(player, held);
            economy.deposit(player.getUniqueId(), value);
            player.sendMessage(ChatColor.GREEN + "Tu as empoché " + MoneyFormat.format(value)
                    + plugin.getConfig().getString("economy.currency-symbol", "") + ".");
            return;
        }

        if (!creative && (value == null || !Double.isFinite(value) || value < 10.0)) {
            event.setCancelled(true);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            player.sendMessage(ChatColor.RED + "En survie, un sac doit contenir au moins 10 pour être posé.");
            return;
        }

        if (action != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            event.setCancelled(true);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            player.sendMessage(ChatColor.RED + "Fais clic droit sur un bloc pour poser le sac.");
            return;
        }

        if (value == null || !Double.isFinite(value) || value < 0) value = 0.0;
        pendingPlacements.put(player.getUniqueId(), new PendingBag(value, creative && value <= 0.0));
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                pendingPlacements.remove(player.getUniqueId()), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemsAdderFurniturePlaced(FurniturePlaceSuccessEvent event) {
        String furnitureId = event.getNamespacedID();
        if (furnitureId == null || !getFurnitureId().equalsIgnoreCase(furnitureId)) return;

        PendingBag pending = pendingPlacements.remove(event.getPlayer().getUniqueId());
        if (pending == null) return;

        Entity entity = event.getBukkitEntity();
        if (entity == null) {
            CustomFurniture furniture = event.getFurniture();
            if (furniture != null) entity = furniture.getEntity();
        }
        if (entity == null || !entity.isValid()) return;

        PersistentDataContainer data = entity.getPersistentDataContainer();
        data.set(placedBagKey, PersistentDataType.DOUBLE, pending.value());
        if (pending.creativeOnly()) {
            data.set(creativeBagKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            data.remove(creativeBagKey);
        }
        entity.setCustomName(null);
        entity.setCustomNameVisible(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCollectPlacedBag(FurnitureBreakEvent event) {
        String furnitureId = event.getNamespacedID();
        if (furnitureId == null || !getFurnitureId().equalsIgnoreCase(furnitureId)) return;
        CustomFurniture furniture = event.getFurniture();
        if (furniture == null || furniture.getEntity() == null) return;

        Entity entity = furniture.getEntity();
        PersistentDataContainer data = entity.getPersistentDataContainer();
        if (!data.has(placedBagKey, PersistentDataType.DOUBLE)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player == null) return;

        boolean creativeBag = data.has(creativeBagKey, PersistentDataType.BYTE);
        if (creativeBag && player.getGameMode() != GameMode.CREATIVE) {
            player.sendMessage(ChatColor.RED + "Ce sac créatif ne peut être récupéré qu'en créatif.");
            return;
        }

        Double value = data.get(placedBagKey, PersistentDataType.DOUBLE);
        if (value == null || !Double.isFinite(value) || value < 0) return;

        ItemStack bag = createBag(value);
        if (bag == null) {
            player.sendMessage(ChatColor.RED + "Impossible de récupérer le sac : l'item ItemsAdder est introuvable.");
            return;
        }

        furniture.remove(false);
        var leftovers = player.getInventory().addItem(bag);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage(ChatColor.GREEN + "Sac récupéré dans ton inventaire.");
    }

    private String getFurnitureId() {
        return plugin.getConfig().getString("economy.money-bag-furniture-id", DEFAULT_BAG_ID).trim();
    }

    private ItemStack createBag(double value) {
        CustomStack custom = CustomStack.getInstance(DEFAULT_BAG_ID);
        if (custom == null || custom.getItemStack() == null) return null;
        ItemStack bag = custom.getItemStack().clone();
        bag.setAmount(1);
        ItemMeta meta = bag.getItemMeta();
        if (meta != null) {
            if (value > 0) {
                meta.getPersistentDataContainer().set(valueKey, PersistentDataType.DOUBLE, value);
            } else {
                meta.getPersistentDataContainer().remove(valueKey);
            }
            String valueLine = value > 0
                    ? ChatColor.GRAY + MoneyFormat.format(value) + plugin.getConfig().getString("economy.currency-symbol", "")
                    : ChatColor.GRAY + "Sans valeur (créatif)";
            meta.setLore(List.of(
                    valueLine,
                    ChatColor.YELLOW + "Clic droit : poser le sac",
                    ChatColor.GOLD + "Shift + clic droit : empocher sa valeur"));
            bag.setItemMeta(meta);
        }
        return bag;
    }

    private boolean isBag(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        try {
            CustomStack custom = CustomStack.byItemStack(item);
            return custom != null && (DEFAULT_BAG_ID.equalsIgnoreCase(custom.getNamespacedID())
                    || getFurnitureId().equalsIgnoreCase(custom.getNamespacedID()));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private Double readValue(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta == null ? null : meta.getPersistentDataContainer().get(valueKey, PersistentDataType.DOUBLE);
    }

    private void consumeOne(Player player, ItemStack held) {
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
    }

    private record PendingBag(double value, boolean creativeOnly) {}
}
