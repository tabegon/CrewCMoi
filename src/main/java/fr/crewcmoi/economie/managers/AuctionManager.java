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

/**
 * Gère la logique de l'hôtel des ventes (/ah) : cache des annonces actives,
 * mise en vente, achat et annulation.
 */
public class AuctionManager {

    public enum BuyResult {
        SUCCESS, NOT_FOUND, OWN_ITEM, NOT_ENOUGH_MONEY, INVENTORY_FULL
    }

    /** Intervalle (en ticks) entre deux vérifications des annonces expirées : 5 minutes. */
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

    /**
     * Nombre maximum d'annonces actives simultanées par joueur (config: economy.auction-max-slots).
     */
    public int getMaxSlots() {
        return Math.max(1, plugin.getConfig().getInt("economy.auction-max-slots", 27));
    }

    /**
     * Durée (en jours) avant expiration automatique d'une annonce (config: economy.auction-expire-days).
     */
    public int getExpireDays() {
        return Math.max(1, plugin.getConfig().getInt("economy.auction-expire-days", 3));
    }

    /**
     * Nombre d'annonces actuellement en vente par ce joueur (basé sur le cache local).
     */
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

    /**
     * Recharge le cache des annonces actives depuis la base de données (asynchrone),
     * puis exécute le callback (s'il est fourni) sur le thread principal.
     */
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

    /**
     * Retourne une copie du cache actuel des annonces (accès synchrone, rapide).
     */
    public List<AuctionItem> getCachedAuctions() {
        synchronized (cache) {
            return new ArrayList<>(cache);
        }
    }

    /**
     * Met en vente un objet. Le prix doit être strictement positif.
     * Le callback est appelé sur le thread principal une fois l'annonce créée (ou en échec, avec cache inchangé).
     */
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

    /**
     * Tente d'acheter une annonce. Le résultat est renvoyé via le callback (thread principal).
     */
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
                    Messages.send(onlineSeller, "server.auction-seller-notified", java.util.Map.of("player", buyer.getName()), false);
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    databaseManager.removeAuction(auctionId);
                    refreshCache(() -> callback.accept(BuyResult.SUCCESS));
                });
            });
        });
    }

    /**
     * Annule une annonce et rend l'objet à son vendeur (uniquement s'il en est le propriétaire).
     */
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

    // Méthode pour enregistrer une vente
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

    /**
     * Démarre la tâche périodique qui retire et restitue les annonces expirées
     * (au bout de economy.auction-expire-days jours).
     */
    private void startExpirationTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processExpiredAuctions,
                EXPIRATION_CHECK_PERIOD_TICKS, EXPIRATION_CHECK_PERIOD_TICKS);
    }

    /**
     * Exécuté de manière asynchrone : cherche les annonces expirées, les supprime en base,
     * puis restitue les objets (sur le thread principal).
     */
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

    /**
     * Restitue l'objet d'une annonce expirée à son vendeur : directement dans l'inventaire
     * s'il est en ligne (le surplus est lâché au sol), sinon en attente jusqu'à sa prochaine connexion.
     */
    private void returnExpiredItem(AuctionItem auction) {
        Player online = Bukkit.getPlayer(auction.getSellerUuid());
        if (online != null && online.isOnline()) {
            for (ItemStack leftover : online.getInventory().addItem(auction.getItem().clone()).values()) {
                online.getWorld().dropItem(online.getLocation(), leftover);
            }
            Messages.send(online, "server.auction-expired-returned", Map.of("days", String.valueOf(getExpireDays())), false);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    databaseManager.addPendingReturn(auction.getSellerUuid(), auction.getItem()));
        }
    }

    /**
     * À appeler à la connexion d'un joueur : lui rend les objets de ses annonces expirées
     * pendant qu'il était hors-ligne (s'il y en a).
     */
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
                Messages.send(player, "server.auction-expired-returned-offline", Map.of("count", String.valueOf(items.size())), false);
            });
        });
    }
}
