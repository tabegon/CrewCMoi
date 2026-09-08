package fr.crewcmoi.other.managers;

import fr.crewcmoi.economie.auction.AuctionItem;
import fr.crewcmoi.pvp.database.BountyEntry;
import fr.crewcmoi.pvp.database.BountyTarget;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.database.ClaimFlag;
import fr.crewcmoi.claims.database.ClaimPermission;
import fr.crewcmoi.teleport.database.HomeData;
import fr.crewcmoi.other.database.PlayerData;
import fr.crewcmoi.pvp.database.TeamData;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public interface DatabaseManager {

    void connect();

    void disconnect();

    void init();

    PlayerData loadOrCreatePlayer(UUID uuid, String name);

    void savePlayer(PlayerData data);

    PlayerData getPlayer(UUID uuid);

    PlayerData getPlayerByName(String name);

    List<PlayerData> getTopBalances(int limit);

    int createAuction(UUID sellerUuid, String sellerName, ItemStack item, double price);

    List<AuctionItem> getActiveAuctions();

    AuctionItem getAuction(int id);

    void removeAuction(int id);

    void addPendingReturn(UUID owner, ItemStack item);

    List<ItemStack> takePendingReturns(UUID owner);

    

    int createTeam(String name, UUID ownerUuid, String ownerName);

    boolean addTeamMember(int teamId, UUID playerUuid, String playerName);

    void removeTeamMember(UUID playerUuid);

    void deleteTeam(int teamId);

    TeamData getTeamByPlayer(UUID playerUuid);

    TeamData getTeamByName(String name);

    

    void addBounty(UUID targetUuid, String targetName, UUID contributorUuid, String contributorName, double amount, String reason);

    BountyEntry getBountyEntry(int entryId);

    List<BountyEntry> getPendingReasonedBounties();

    void setBountyApproved(int entryId, boolean approved);

    void deleteBountyEntry(int entryId);

    List<BountyEntry> getBounties(UUID targetUuid);

    List<BountyTarget> getBountyTargets();

    void clearBounties(UUID targetUuid);

    void clearPlayerBounties(UUID targetUuid);

    int getServerBountyCountToday(UUID playerUuid);

    void incrementServerBountyCount(UUID playerUuid);

    

    void setHome(UUID playerUuid, String world, double x, double y, double z, float yaw, float pitch);

    HomeData getHome(UUID playerUuid);

    

    boolean createClaim(String world, int chunkX, int chunkZ, UUID ownerUuid, String ownerName);

    

    boolean removeClaim(String world, int chunkX, int chunkZ);

    ClaimData getClaim(String world, int chunkX, int chunkZ);

    List<ClaimData> getClaimsByOwner(UUID ownerUuid);

    List<ClaimData> getAllClaims();

    void addTrusted(String world, int chunkX, int chunkZ, UUID trustedUuid);

    void removeTrusted(String world, int chunkX, int chunkZ, UUID trustedUuid);

    void setClaimFlag(String world, int chunkX, int chunkZ, ClaimFlag flag, ClaimPermission permission);

    int getExtraClaims(UUID playerUuid);

    void setExtraClaims(UUID playerUuid, int amount);

    void setClaimSalePrice(String world, int chunkX, int chunkZ, double price);

    void transferClaim(String world, int chunkX, int chunkZ, UUID newOwnerUuid, String newOwnerName);
}
