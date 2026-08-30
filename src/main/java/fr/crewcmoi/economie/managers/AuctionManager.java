package fr.crewcmoi.economie.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.auction.AuctionItem;
import fr.crewcmoi.managers.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gère la logique de l'hôtel des ventes (/ah) : cache des annonces actives,
 * mise en vente, achat et annulation.
 */
public class AuctionManager {

    public enum BuyResult {
        SUCCESS, NOT_FOUND, OWN_ITEM, NOT_ENOUGH_MONEY, INVENTORY_FULL
    }

    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;

    private final List<AuctionItem> cache = Collections.synchronizedList(new ArrayList<>());

    public AuctionManager(Main plugin, DatabaseManager databaseManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.economyManager = economyManager;
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
                    onlineSeller.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&8[&6Economy&8] &r&aVotre annonce a été achetée par &e" + buyer.getName() + "&a."));
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
}
