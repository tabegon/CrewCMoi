package fr.crewcmoi.managers;

import fr.crewcmoi.auction.AuctionItem;
import fr.crewcmoi.database.BountyEntry;
import fr.crewcmoi.database.BountyTarget;
import fr.crewcmoi.database.PlayerData;
import fr.crewcmoi.database.TeamData;
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

    // ===================== TEAMS =====================

    /**
     * Crée une nouvelle équipe avec le joueur donné comme propriétaire (et premier membre).
     * Retourne l'identifiant de l'équipe créée, ou -1 en cas d'échec (ex: nom déjà pris).
     */
    int createTeam(String name, UUID ownerUuid, String ownerName);

    /**
     * Ajoute un membre à une équipe existante.
     */
    boolean addTeamMember(int teamId, UUID playerUuid, String playerName);

    /**
     * Retire un joueur de l'équipe dont il fait partie (n'a aucun effet s'il n'est dans aucune équipe).
     */
    void removeTeamMember(UUID playerUuid);

    /**
     * Supprime totalement une équipe (et tous ses membres).
     */
    void deleteTeam(int teamId);

    /**
     * Retourne l'équipe dont fait partie le joueur, ou null s'il n'en a pas.
     */
    TeamData getTeamByPlayer(UUID playerUuid);

    /**
     * Retourne l'équipe portant ce nom (insensible à la casse), ou null si elle n'existe pas.
     */
    TeamData getTeamByName(String name);

    // ===================== BOUNTIES =====================

    /**
     * Ajoute une contribution à la prime d'un joueur. contributorUuid == null signifie
     * qu'il s'agit d'une prime attribuée automatiquement par le serveur.
     */
    void addBounty(UUID targetUuid, String targetName, UUID contributorUuid, String contributorName, double amount);

    /**
     * Retourne toutes les contributions de prime pour un joueur donné.
     */
    List<BountyEntry> getBounties(UUID targetUuid);

    /**
     * Retourne la liste des joueurs ayant une prime active, avec le montant total cumulé,
     * triée par montant décroissant.
     */
    List<BountyTarget> getBountyTargets();

    /**
     * Supprime toutes les contributions de prime d'un joueur (ex: remise à zéro complète,
     * utilisable par une commande admin).
     */
    void clearBounties(UUID targetUuid);

    /**
     * Supprime uniquement les contributions de prime placées par des JOUEURS (contributor_uuid
     * non nul) sur un joueur, en conservant sa éventuelle prime SERVEUR. À utiliser quand la
     * prime "joueur" vient d'être réclamée (le joueur meurt), sans effacer son statut de
     * prime serveur (qui doit persister à travers ses morts).
     */
    void clearPlayerBounties(UUID targetUuid);

    /**
     * Retourne le nombre de fois où la prime serveur a été attribuée à ce joueur aujourd'hui.
     */
    int getServerBountyCountToday(UUID playerUuid);

    /**
     * Incrémente le compteur journalier de prime serveur pour ce joueur.
     */
    void incrementServerBountyCount(UUID playerUuid);
}
