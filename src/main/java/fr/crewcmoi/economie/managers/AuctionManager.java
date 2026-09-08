package fr.crewcmoi.economie.managers;

import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.auction.AuctionItem;
import fr.crewcmoi.other.managers.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AuctionManager {

    public enum BuyResult {
        SUCCESS, NOT_FOUND, OWN_ITEM, NOT_ENOUGH_MONEY, INVENTORY_FULL
    }

    private static final long EXPIRATION_CHECK_PERIOD_TICKS = 20L * 60L * 5L;

    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;

    private final List<AuctionItem> cache = Collections.synchronizedList(new ArrayList<>());

    public AuctionManager(Main plugin, DatabaseManager databaseManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.economyManager = economyManager;
        startExpirationTask();
    }

    public int getMaxSlots() {
        return Math.max(1, plugin.getConfig().getInt("economy.auction-max-slots", 27));
    }

    public int getExpireDays() {
        return Math.max(1, plugin.getConfig().getInt("economy.auction-expire-days", 3));
    }

    public int countActiveAuctions(UUID sellerUuid) {
        synchronized (cache) {
            int count = 0;
            for (AuctionItem auction : cache) {
                if (auction.getSellerUuid().equals(sellerUuid)) {
                    count++;
                }
            }
            return count;
        }
    }

    public void refreshCache(Runnable callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<AuctionItem> fresh = databaseManager.getActiveAuctions();
            synchronized (cache) {
                cache.clear();
                cache.addAll(fresh);
            }
            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, callback);
            }
        });
    }

    public List<AuctionItem> getCachedAuctions() {
        synchronized (cache) {
            return new ArrayList<>(cache);
        }
    }

    public void listItem(Player seller, ItemStack item, double price, Consumer<Boolean> callback) {
        ItemStack toStore = item.clone();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int id = databaseManager.createAuction(seller.getUniqueId(), seller.getName(), toStore, price);
            if (id > 0) {
                refreshCache(() -> callback.accept(true));
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    public void buy(Player buyer, int auctionId, Consumer<BuyResult> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            AuctionItem auction = databaseManager.getAuction(auctionId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (auction == null) {
                    refreshCache(null);
                    callback.accept(BuyResult.NOT_FOUND);
                    return;
                }

                if (auction.getSellerUuid().equals(buyer.getUniqueId())) {
                    callback.accept(BuyResult.OWN_ITEM);
                    return;
                }

                if (!economyManager.has(buyer.getUniqueId(), auction.getPrice())) {
                    callback.accept(BuyResult.NOT_ENOUGH_MONEY);
                    return;
                }

                if (buyer.getInventory().firstEmpty() == -1) {
                    callback.accept(BuyResult.INVENTORY_FULL);
                    return;
                }

                economyManager.withdraw(buyer.getUniqueId(), auction.getPrice());

                double taxPercent = plugin.getConfig().getDouble("economy.auction-tax-percent", 0.0);
                double tax = auction.getPrice() * (Math.max(0, taxPercent) / 100.0);
                double sellerGain = Math.max(0, auction.getPrice() - tax);

                economyManager.deposit(auction.getSellerUuid(), sellerGain);
                buyer.getInventory().addItem(auction.getItem());

                Player onlineSeller = Bukkit.getPlayer(auction.getSellerUuid());
                if (onlineSeller != null) {
                    Messages.send(onlineSeller, "economy.auction.seller-notified", java.util.Map.of("player", buyer.getName()), false);
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    databaseManager.removeAuction(auctionId);
                    refreshCache(() -> callback.accept(BuyResult.SUCCESS));
                });
            });
        });
    }

    public void cancel(Player seller, int auctionId, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            AuctionItem auction = databaseManager.getAuction(auctionId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (auction == null || !auction.getSellerUuid().equals(seller.getUniqueId())) {
                    if (callback != null) callback.accept(false);
                    return;
                }

                for (ItemStack leftover : seller.getInventory().addItem(auction.getItem()).values()) {
                    seller.getWorld().dropItem(seller.getLocation(), leftover);
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    databaseManager.removeAuction(auctionId);
                    refreshCache(() -> {
                        if (callback != null) callback.accept(true);
                    });
                });
            });
        });
    }
    public List<AuctionItem> getItems() {
        return getCachedAuctions();
    }
    private Map<String, Object> lastSaleData = null;

    public void recordSale(String itemName, int amount, double price, String buyerName, String sellerName) {
        Map<String, Object> sale = new java.util.HashMap<>();
        sale.put("item", itemName);
        sale.put("amount", amount);
        sale.put("price", price);
        sale.put("buyer", buyerName);
        sale.put("seller", sellerName);
        this.lastSaleData = sale;
    }

    public Map<String, Object> getLastSaleData() {
        return lastSaleData;
    }

    private void startExpirationTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processExpiredAuctions,
                EXPIRATION_CHECK_PERIOD_TICKS, EXPIRATION_CHECK_PERIOD_TICKS);
    }

    private void processExpiredAuctions() {
        long expireMillis = TimeUnit.DAYS.toMillis(getExpireDays());
        long now = System.currentTimeMillis();

        List<AuctionItem> fresh = databaseManager.getActiveAuctions();
        List<AuctionItem> expired = new ArrayList<>();
        for (AuctionItem auction : fresh) {
            if (now - auction.getCreatedAt() >= expireMillis) {
                expired.add(auction);
            }
        }

        if (expired.isEmpty()) {
            return;
        }

        for (AuctionItem auction : expired) {
            databaseManager.removeAuction(auction.getId());
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (AuctionItem auction : expired) {
                returnExpiredItem(auction);
            }
            refreshCache(null);
        });
    }

    private void returnExpiredItem(AuctionItem auction) {
        Player online = Bukkit.getPlayer(auction.getSellerUuid());
        if (online != null && online.isOnline()) {
            for (ItemStack leftover : online.getInventory().addItem(auction.getItem().clone()).values()) {
                online.getWorld().dropItem(online.getLocation(), leftover);
            }
            Messages.send(online, "economy.auction.expired-returned", Map.of("days", String.valueOf(getExpireDays())), false);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    databaseManager.addPendingReturn(auction.getSellerUuid(), auction.getItem()));
        }
    }

    public void deliverPendingReturns(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ItemStack> items = databaseManager.takePendingReturns(player.getUniqueId());
            if (items.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (ItemStack item : items) {
                    for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                        player.getWorld().dropItem(player.getLocation(), leftover);
                    }
                }
                Messages.send(player, "economy.auction.expired-returned-offline", Map.of("count", String.valueOf(items.size())), false);
            });
        });
    }
}
