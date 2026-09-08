package fr.crewcmoi.claims.database;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClaimData {

    private final String world;
    private final int chunkX;
    private final int chunkZ;
    private final UUID ownerUuid;
    private final String ownerName;
    private final Set<UUID> trusted;
    private final Map<ClaimFlag, ClaimPermission> flags;

    private final double salePrice;

    public ClaimData(String world, int chunkX, int chunkZ, UUID ownerUuid, String ownerName, Set<UUID> trusted) {
        this(world, chunkX, chunkZ, ownerUuid, ownerName, trusted, defaultFlags());
    }

    public ClaimData(String world, int chunkX, int chunkZ, UUID ownerUuid, String ownerName,
                      Set<UUID> trusted, Map<ClaimFlag, ClaimPermission> flags) {
        this(world, chunkX, chunkZ, ownerUuid, ownerName, trusted, flags, -1);
    }

    public ClaimData(String world, int chunkX, int chunkZ, UUID ownerUuid, String ownerName,
                      Set<UUID> trusted, Map<ClaimFlag, ClaimPermission> flags, double salePrice) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.trusted = trusted;
        this.flags = flags;
        this.salePrice = salePrice;
    }

    public static Map<ClaimFlag, ClaimPermission> defaultFlags() {
        Map<ClaimFlag, ClaimPermission> map = new EnumMap<>(ClaimFlag.class);
        for (ClaimFlag flag : ClaimFlag.values()) {
            map.put(flag, ClaimPermission.TRUSTED);
        }
        return map;
    }

    public Map<ClaimFlag, ClaimPermission> getFlags() {
        return flags;
    }

    public ClaimPermission getPermission(ClaimFlag flag) {
        return flags.getOrDefault(flag, ClaimPermission.TRUSTED);
    }

    public Set<UUID> getTrusted() {
        return trusted;
    }

    public boolean canBuild(UUID playerUuid) {
        return ownerUuid.equals(playerUuid) || trusted.contains(playerUuid);
    }

    public boolean isAllowed(UUID playerUuid, ClaimFlag flag) {
        if (ownerUuid.equals(playerUuid)) {
            return true;
        }
        ClaimPermission permission = getPermission(flag);
        return switch (permission) {
            case EVERYONE -> true;
            case TRUSTED -> trusted.contains(playerUuid);
            case OWNER_ONLY -> false;
        };
    }

    public boolean isOpen(ClaimFlag flag) {
        return getPermission(flag) == ClaimPermission.EVERYONE;
    }

    public String getWorld() {
        return world;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public double getSalePrice() {
        return salePrice;
    }

    public boolean isForSale() {
        return salePrice >= 0;
    }
}
