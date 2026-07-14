package fr.crewcmoi.managers;

import fr.crewcmoi.auction.AuctionItem;
import fr.crewcmoi.database.PlayerData;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Interface générique pour la gestion de la base de données.
 * Permet de changer facilement d'implémentation (SQLite, MySQL, etc.)
 */
public interface DatabaseManager {

    void connect();

    void disconnect();

    void init();

    /**
     * Charge les données d'un joueur, ou les crée si elles n'existent pas.
     */
    PlayerData loadOrCreatePlayer(UUID uuid, String name);

    /**
     * Sauvegarde les données d'un joueur en base.
     */
    void savePlayer(PlayerData data);

    /**
     * Récupère les données d'un joueur en base (peut retourner null).
     */
    PlayerData getPlayer(UUID uuid);

    /**
     * Récupère les données d'un joueur via son pseudo (peut retourner null).
     */
    PlayerData getPlayerByName(String name);

    /**
     * Retourne la liste de tous les joueurs triés par solde décroissant.
     */
    List<PlayerData> getTopBalances(int limit);

    /**
     * Crée une nouvelle annonce dans l'hôtel des ventes et retourne son identifiant (-1 en cas d'échec).
     */
    int createAuction(UUID sellerUuid, String sellerName, ItemStack item, double price);

    /**
     * Retourne toutes les annonces actives de l'hôtel des ventes.
     */
    List<AuctionItem> getActiveAuctions();

    /**
     * Récupère une annonce par son identifiant (peut retourner null si elle n'existe plus).
     */
    AuctionItem getAuction(int id);

    /**
     * Supprime une annonce (vendue ou annulée).
     */
    void removeAuction(int id);
}
