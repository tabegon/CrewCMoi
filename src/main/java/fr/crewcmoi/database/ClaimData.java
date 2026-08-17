package fr.crewcmoi.database;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Représente un chunk claim par un joueur : personne d'autre que le propriétaire
 * (ou un membre de confiance) ne peut y construire, détruire, ou l'endommager,
 * selon les règles configurées via /claim settings.
 */
public class ClaimData {

    private final String world;
    private final int chunkX;
    private final int chunkZ;
    private final UUID ownerUuid;
    private final String ownerName;
    private final Set<UUID> trusted;
    private final Map<ClaimFlag, ClaimPermission> flags;
    // Prix de vente si le propriétaire a mis ce claim en vente via /claim sell, sinon -1
    // (claim non à vendre).
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

    /**
     * Règles par défaut d'un nouveau claim : le propriétaire et les joueurs de confiance
     * peuvent tout faire, tous les autres joueurs sont bloqués.
     */
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

    /**
     * Un joueur peut modifier ce chunk s'il en est le propriétaire ou s'il fait partie
     * des joueurs de confiance ajoutés par le propriétaire.
     */
    public boolean canBuild(UUID playerUuid) {
        return ownerUuid.equals(playerUuid) || trusted.contains(playerUuid);
    }

    /**
     * Vérifie si un joueur donné est autorisé à effectuer l'action régie par la règle
     * indiquée, en tenant compte du niveau configuré (propriétaire / confiance / tout le monde).
     */
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

    /**
     * Vrai si la règle est totalement ouverte (aucune restriction, même pour un acteur
     * non-joueur comme un creeper ou la propagation du feu).
     */
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

    /**
     * Prix de vente demandé par le propriétaire, ou -1 si ce claim n'est pas à vendre.
     */
    public double getSalePrice() {
        return salePrice;
    }

    public boolean isForSale() {
        return salePrice >= 0;
    }
}
