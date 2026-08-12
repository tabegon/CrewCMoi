package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.ClaimData;
import fr.crewcmoi.database.ClaimFlag;
import fr.crewcmoi.database.ClaimPermission;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère le système de claims de chunks : un joueur peut réclamer le chunk sur lequel il
 * se trouve via /claim, empêchant tout autre joueur (non autorisé) de construire, détruire,
 * mettre le feu, faire exploser, etc. dans ce chunk.
 *
 * Toutes les données sont mises en cache en mémoire (chargées au démarrage depuis la base de
 * données) pour que les vérifications de protection dans les listeners restent instantanées.
 */
public class ClaimManager {

    private final Main plugin;
    private final DatabaseManager databaseManager;

    // Clé "monde;chunkX;chunkZ" -> claim
    private final Map<String, ClaimData> claims = new ConcurrentHashMap<>();

    public ClaimManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Charge tous les claims en mémoire. À appeler au démarrage du plugin (de façon synchrone,
     * avant que des joueurs ne se connectent).
     */
    public void loadAll() {
        for (ClaimData claim : databaseManager.getAllClaims()) {
            claims.put(key(claim.getWorld(), claim.getChunkX(), claim.getChunkZ()), claim);
        }
        plugin.getLogger().info(claims.size() + " claim(s) chargé(s).");
    }

    private String key(String world, int chunkX, int chunkZ) {
        return world + ";" + chunkX + ";" + chunkZ;
    }

    /**
     * Tente de claim le chunk sur lequel se trouve le joueur.
     * Retourne : SUCCESS, ALREADY_CLAIMED (par n'importe qui, y compris lui-même), ou LIMIT_REACHED.
     */
    public ClaimResult claim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        if (getClaim(world, chunkX, chunkZ) != null) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        int max = plugin.getConfig().getInt("claims.max-per-player", 0);
        if (max > 0 && !player.hasPermission("crew.claim.bypasslimit")) {
            long owned = claims.values().stream()
                    .filter(c -> c.getOwnerUuid().equals(player.getUniqueId()))
                    .count();
            if (owned >= max) {
                return ClaimResult.LIMIT_REACHED;
            }
        }

        boolean created = databaseManager.createClaim(world, chunkX, chunkZ, player.getUniqueId(), player.getName());
        if (!created) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        claims.put(key(world, chunkX, chunkZ), new ClaimData(world, chunkX, chunkZ,
                player.getUniqueId(), player.getName(), java.util.Collections.emptySet()));
        return ClaimResult.SUCCESS;
    }

    /**
     * Retire le claim du chunk sur lequel se trouve le joueur, s'il en est le propriétaire
     * (ou s'il a la permission admin de bypass).
     */
    public UnclaimResult unclaim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        ClaimData claim = getClaim(world, chunkX, chunkZ);
        if (claim == null) {
            return UnclaimResult.NOT_CLAIMED;
        }
        if (!claim.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("crew.claim.admin")) {
            return UnclaimResult.NOT_OWNER;
        }

        databaseManager.removeClaim(world, chunkX, chunkZ);
        claims.remove(key(world, chunkX, chunkZ));
        return UnclaimResult.SUCCESS;
    }

    /**
     * Ajoute un joueur de confiance au claim du chunk où se trouve le propriétaire.
     */
    public TrustResult trust(Player player, UUID trustedUuid, String trustedName) {
        Chunk chunk = player.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        ClaimData claim = getClaim(world, chunkX, chunkZ);
        if (claim == null) {
            return TrustResult.NOT_CLAIMED;
        }
        if (!claim.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("crew.claim.admin")) {
            return TrustResult.NOT_OWNER;
        }

        databaseManager.addTrusted(world, chunkX, chunkZ, trustedUuid);
        java.util.Set<UUID> trusted = new java.util.LinkedHashSet<>(claim.getTrusted());
        trusted.add(trustedUuid);
        claims.put(key(world, chunkX, chunkZ), new ClaimData(world, chunkX, chunkZ,
                claim.getOwnerUuid(), claim.getOwnerName(), trusted));
        return TrustResult.SUCCESS;
    }

    /**
     * Retire un joueur de confiance du claim du chunk où se trouve le propriétaire.
     */
    public TrustResult untrust(Player player, UUID trustedUuid) {
        Chunk chunk = player.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        ClaimData claim = getClaim(world, chunkX, chunkZ);
        if (claim == null) {
            return TrustResult.NOT_CLAIMED;
        }
        if (!claim.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("crew.claim.admin")) {
            return TrustResult.NOT_OWNER;
        }

        databaseManager.removeTrusted(world, chunkX, chunkZ, trustedUuid);
        java.util.Set<UUID> trusted = new java.util.LinkedHashSet<>(claim.getTrusted());
        trusted.remove(trustedUuid);
        claims.put(key(world, chunkX, chunkZ), new ClaimData(world, chunkX, chunkZ,
                claim.getOwnerUuid(), claim.getOwnerName(), trusted));
        return TrustResult.SUCCESS;
    }

    /**
     * Modifie le niveau de permission d'une règle pour le claim donné (utilisé par la GUI
     * /claims settings, où le chunk édité doit être celui affiché, pas forcément celui où
     * se trouve encore le joueur).
     */
    public TrustResult setFlag(Player player, String world, int chunkX, int chunkZ,
                                ClaimFlag flag, ClaimPermission permission) {
        ClaimData claim = getClaim(world, chunkX, chunkZ);
        if (claim == null) {
            return TrustResult.NOT_CLAIMED;
        }
        if (!claim.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("crew.claim.admin")) {
            return TrustResult.NOT_OWNER;
        }

        databaseManager.setClaimFlag(world, chunkX, chunkZ, flag, permission);
        java.util.Map<ClaimFlag, ClaimPermission> flags = new java.util.EnumMap<>(claim.getFlags());
        flags.put(flag, permission);
        claims.put(key(world, chunkX, chunkZ), new ClaimData(world, chunkX, chunkZ,
                claim.getOwnerUuid(), claim.getOwnerName(), claim.getTrusted(), flags));
        return TrustResult.SUCCESS;
    }

    public ClaimData getClaim(String world, int chunkX, int chunkZ) {
        return claims.get(key(world, chunkX, chunkZ));
    }

    public ClaimData getClaim(Chunk chunk) {
        return getClaim(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public ClaimData getClaim(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return getClaim(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    /**
     * Retourne true si le joueur peut construire/détruire à cet endroit : soit le chunk
     * n'est pas claim, soit le joueur en est le propriétaire ou est de confiance, soit il
     * a la permission de bypass admin.
     */
    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission("crew.claim.bypass")) {
            return true;
        }
        ClaimData claim = getClaim(location);
        return claim == null || claim.canBuild(player.getUniqueId());
    }

    /**
     * Vérifie si un joueur est autorisé à effectuer l'action régie par la règle indiquée
     * à cet endroit, en tenant compte du niveau de permission configuré via /claims settings.
     */
    public boolean isAllowed(Player player, Location location, ClaimFlag flag) {
        if (player.hasPermission("crew.claim.bypass")) {
            return true;
        }
        ClaimData claim = getClaim(location);
        return claim == null || claim.isAllowed(player.getUniqueId(), flag);
    }

    /**
     * Vérifie si une règle est totalement ouverte à cet endroit (aucune restriction, y
     * compris pour des acteurs non-joueurs comme les creepers, la foudre ou le feu qui se
     * propage naturellement). Retourne true si le chunk n'est pas claim.
     */
    public boolean isOpen(Location location, ClaimFlag flag) {
        ClaimData claim = getClaim(location);
        return claim == null || claim.isOpen(flag);
    }

    public enum ClaimResult {
        SUCCESS, ALREADY_CLAIMED, LIMIT_REACHED
    }

    public enum UnclaimResult {
        SUCCESS, NOT_CLAIMED, NOT_OWNER
    }

    public enum TrustResult {
        SUCCESS, NOT_CLAIMED, NOT_OWNER
    }
}
