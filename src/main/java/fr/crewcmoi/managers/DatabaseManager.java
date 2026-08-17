package fr.crewcmoi.managers;

import fr.crewcmoi.auction.AuctionItem;
import fr.crewcmoi.database.BountyEntry;
import fr.crewcmoi.database.BountyTarget;
import fr.crewcmoi.database.ClaimData;
import fr.crewcmoi.database.ClaimFlag;
import fr.crewcmoi.database.ClaimPermission;
import fr.crewcmoi.database.HomeData;
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
    void addBounty(UUID targetUuid, String targetName, UUID contributorUuid, String contributorName, double amount, String reason);

    /**
     * Retourne une contribution de prime précise par son id, ou null si introuvable.
     */
    BountyEntry getBountyEntry(int entryId);

    /**
     * Retourne toutes les contributions de prime avec une raison fournie par un joueur,
     * pas encore approuvées par un admin (voir /bounty review).
     */
    List<BountyEntry> getPendingReasonedBounties();

    /**
     * Marque une contribution de prime comme approuvée (ou non) par un admin.
     */
    void setBountyApproved(int entryId, boolean approved);

    /**
     * Supprime une contribution de prime précise (utilisé pour refuser une raison).
     */
    void deleteBountyEntry(int entryId);

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

    // ===================== HOMES =====================

    /**
     * Crée ou met à jour le home d'un joueur (un seul home par joueur).
     */
    void setHome(UUID playerUuid, String world, double x, double y, double z, float yaw, float pitch);

    /**
     * Récupère le home d'un joueur, ou null s'il n'en a pas défini.
     */
    HomeData getHome(UUID playerUuid);

    // ===================== CLAIMS =====================

    /**
     * Crée un claim pour ce chunk. Retourne false si le chunk est déjà claim.
     */
    boolean createClaim(String world, int chunkX, int chunkZ, UUID ownerUuid, String ownerName);

    /**
     * Supprime le claim de ce chunk (aucun effet s'il n'était pas claim).
     */
    /**
     * Supprime le claim donné. Retourne true si la suppression a bien été effectuée en
     * base de données (au moins une ligne affectée), false en cas d'erreur SQL ou si la
     * ligne n'existait déjà plus. Le retour DOIT être vérifié par l'appelant avant de
     * considérer l'opération comme réussie : voir ClaimManager#unclaim.
     */
    boolean removeClaim(String world, int chunkX, int chunkZ);

    /**
     * Retourne le claim de ce chunk, ou null s'il n'est pas claim.
     */
    ClaimData getClaim(String world, int chunkX, int chunkZ);

    /**
     * Retourne tous les claims appartenant à ce joueur.
     */
    List<ClaimData> getClaimsByOwner(UUID ownerUuid);

    /**
     * Retourne tous les claims existants (utilisé pour peupler le cache mémoire au démarrage).
     */
    List<ClaimData> getAllClaims();

    /**
     * Ajoute un joueur de confiance à un claim (peut construire/détruire dessus).
     */
    void addTrusted(String world, int chunkX, int chunkZ, UUID trustedUuid);

    /**
     * Retire un joueur de confiance d'un claim.
     */
    void removeTrusted(String world, int chunkX, int chunkZ, UUID trustedUuid);

    /**
     * Modifie le niveau de permission d'une règle (flag) pour ce claim.
     */
    void setClaimFlag(String world, int chunkX, int chunkZ, ClaimFlag flag, ClaimPermission permission);

    /**
     * Retourne le nombre de claims supplémentaires achetés par ce joueur (en plus du
     * quota gratuit défini dans la config), 0 s'il n'en a jamais acheté.
     */
    int getExtraClaims(UUID playerUuid);

    /**
     * Définit le nombre de claims supplémentaires achetés par ce joueur.
     */
    void setExtraClaims(UUID playerUuid, int amount);

    /**
     * Met (ou retire, avec price = -1) ce claim en vente pour le prix donné.
     */
    void setClaimSalePrice(String world, int chunkX, int chunkZ, double price);

    /**
     * Transfère la propriété d'un claim à un nouveau propriétaire (achat via /claim buy) :
     * met à jour le propriétaire, retire la mise en vente et réinitialise les joueurs de
     * confiance (les anciens joueurs de confiance du vendeur n'ont pas vocation à le rester).
     */
    void transferClaim(String world, int chunkX, int chunkZ, UUID newOwnerUuid, String newOwnerName);
}
