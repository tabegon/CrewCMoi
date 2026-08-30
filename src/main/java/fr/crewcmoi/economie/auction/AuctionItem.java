package fr.crewcmoi.economie.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Représente une annonce de l'hôtel des ventes (/ah).
 */
public class AuctionItem {

    private final int id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long createdAt;

    public AuctionItem(int id, UUID sellerUuid, String sellerName, ItemStack item, double price, long createdAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
