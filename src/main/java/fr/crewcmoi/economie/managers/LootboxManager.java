package fr.crewcmoi.economie.managers;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class LootboxManager implements org.bukkit.event.Listener {
    private static final int[] REWARD_SLOTS = {11, 13, 15};
    private final Main plugin;
    private final Random random = new Random();
    private final java.util.Map<UUID, BukkitTask> animations = new java.util.HashMap<>();

    public LootboxManager(Main plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (animations.containsKey(player.getUniqueId())) {
            return;
        }

        List<PoolEntry> pool = loadPool();
        if (pool.isEmpty()) {
            String message = plugin.getConfig().getString("lootbox.messages.empty-pool", "&cLa pool de la lootbox est vide ou mal configurée.");
            player.sendMessage(Messages.color(message));
            return;
        }

        if (!consumeKey(player)) {
            String message = plugin.getConfig().getString("lootbox.messages.no-key", "&cVous avez besoin d'une clé de lootbox.");
            player.sendMessage(Messages.color(message));
            return;
        }

        LootboxHolder holder = new LootboxHolder();
        Inventory gui = Bukkit.createInventory(holder, 27,
                Messages.color(plugin.getConfig().getString("lootbox.gui-title", "&5&lLootbox")));
        holder.setInventory(gui);
        fillBackground(gui);
        gui.setItem(4, namedItem(Material.CHEST, plugin.getConfig().getString("lootbox.gui-header", "&d&lOuverture de la lootbox")));
        gui.setItem(22, namedItem(Material.NETHER_STAR, plugin.getConfig().getString("lootbox.gui-footer", "&7Récompenses en cours...")));
        player.openInventory(gui);

        int duration = Math.max(20, plugin.getConfig().getInt("lootbox.animation.duration-ticks", 80));
        int interval = Math.max(1, plugin.getConfig().getInt("lootbox.animation.interval-ticks", 4));
        int totalSteps = Math.max(1, duration / interval);

        BukkitTask task = new BukkitRunnable() {
            int step = 0;
            final ItemStack[] finalRewards = new ItemStack[3];

            @Override
            public void run() {
                if (!player.isOnline()) {
                    finishOffline(player, finalRewards, pool);
                    animations.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                step++;
                boolean last = step >= totalSteps;

                for (int i = 0; i < REWARD_SLOTS.length; i++) {
                    ItemStack reward = last ? choose(pool) : choose(pool);
                    if (last) {
                        finalRewards[i] = reward.clone();
                    }
                    gui.setItem(REWARD_SLOTS[i], reward);
                }

                player.playSound(player.getLocation(), last ? Sound.ENTITY_PLAYER_LEVELUP : Sound.BLOCK_NOTE_BLOCK_PLING,
                        0.8f, last ? 1.2f : 1.8f);

                if (last) {
                    for (int i = 0; i < finalRewards.length; i++) {
                        giveOrDrop(player, finalRewards[i]);
                    }
                    player.sendMessage(Messages.color(plugin.getConfig().getString("lootbox.messages.won", "&aVous avez gagné 3 récompenses !")));
                    player.closeInventory();
                    animations.remove(player.getUniqueId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, interval);

        animations.put(player.getUniqueId(), task);
    }

    public void cancel(Player player) {
        BukkitTask task = animations.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private boolean consumeKey(Player player) {
        String id = plugin.getConfig().getString("lootbox.key-itemsadder-id", "").trim();
        if (id.isEmpty()) return false;

        ItemStack main = player.getInventory().getItemInMainHand();
        if (isCustomItem(main, id)) {
            removeOne(player, true);
            return true;
        }

        ItemStack off = player.getInventory().getItemInOffHand();
        if (isCustomItem(off, id)) {
            removeOne(player, false);
            return true;
        }
        return false;
    }

    private void removeOne(Player player, boolean mainHand) {
        ItemStack item = mainHand ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        if (item.getAmount() <= 1) {
            if (mainHand) player.getInventory().setItemInMainHand(null);
            else player.getInventory().setItemInOffHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private boolean isCustomItem(ItemStack item, String configuredId) {
        if (item == null || item.getType().isAir()) return false;
        CustomStack custom = CustomStack.byItemStack(item);
        return custom != null && configuredId.equalsIgnoreCase(custom.getNamespacedID());
    }

    private List<PoolEntry> loadPool() {
        List<PoolEntry> result = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("lootbox.pool");
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            String id = section.getString(key + ".itemsadder-id", "").trim();
            if (id.isEmpty()) continue;
            int amount = Math.max(1, section.getInt(key + ".amount", 1));
            double weight = Math.max(0.0, section.getDouble(key + ".weight", 1.0));
            CustomStack custom = CustomStack.getInstance(id);
            if (custom == null || custom.getItemStack() == null || weight <= 0) {
                plugin.getLogger().warning("Lootbox : item ItemsAdder introuvable ou invalide : " + id);
                continue;
            }
            result.add(new PoolEntry(custom.getItemStack(), amount, weight));
        }
        return result;
    }

    private ItemStack choose(List<PoolEntry> pool) {
        double total = 0.0;
        for (PoolEntry entry : pool) total += entry.weight;
        double value = random.nextDouble() * total;
        for (PoolEntry entry : pool) {
            value -= entry.weight;
            if (value <= 0) {
                ItemStack item = entry.item.clone();
                item.setAmount(Math.min(item.getMaxStackSize(), entry.amount));
                return item;
            }
        }
        PoolEntry last = pool.get(pool.size() - 1);
        ItemStack item = last.item.clone();
        item.setAmount(Math.min(item.getMaxStackSize(), last.amount));
        return item;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }

    private void finishOffline(Player player, ItemStack[] finalRewards, List<PoolEntry> pool) {
        for (int i = 0; i < finalRewards.length; i++) {
            if (finalRewards[i] == null) finalRewards[i] = choose(pool);
        }
        for (ItemStack reward : finalRewards) {
            if (reward != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), reward);
            }
        }
        plugin.getLogger().info("Lootbox : " + player.getName() + " s'est déconnecté pendant l'animation. Les récompenses ont été déposées à sa dernière position.");
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = namedItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
        for (int slot : REWARD_SLOTS) inventory.setItem(slot, null);
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @org.bukkit.event.EventHandler
    public void onLootboxClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof LootboxHolder) {
            event.setCancelled(true);
        }
    }

    @org.bukkit.event.EventHandler
    public void onLootboxDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof LootboxHolder) {
            event.setCancelled(true);
        }
    }

    private record PoolEntry(ItemStack item, int amount, double weight) {}
}
