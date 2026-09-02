package fr.crewcmoi.claims.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.database.ClaimFlag;
import fr.crewcmoi.claims.database.ClaimPermission;
import fr.crewcmoi.other.managers.DatabaseManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
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

    // Cache des claims supplémentaires achetés par joueur (UUID -> nombre acheté).
    private final Map<UUID, Integer> extraClaims = new ConcurrentHashMap<>();

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

        int max = getMaxClaims(player);
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
            // Le chunk est considéré libre par le cache mais la base le refuse (déjà présent) :
            // cas typique d'une désynchronisation cache/DB causée par un ancien /claim unclaim
            // qui avait échoué silencieusement en base (voir le correctif dans unclaim() plus
            // bas). On resynchronise le cache avec la réalité de la base plutôt que de laisser
            // ce chunk éternellement "fantôme" (libre en cache, mais jamais re-claimable).
            ClaimData realClaim = databaseManager.getClaim(world, chunkX, chunkZ);
            if (realClaim != null) {
                claims.put(key(world, chunkX, chunkZ), realClaim);
            }
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

        ClaimData claim = getClaimResynced(world, chunkX, chunkZ);
        if (claim == null) {
            return UnclaimResult.NOT_CLAIMED;
        }
        if (!claim.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("crew.claim.admin")) {
            return UnclaimResult.NOT_OWNER;
        }

        boolean removed = databaseManager.removeClaim(world, chunkX, chunkZ);
        if (!removed) {
            // La suppression en base a échoué (ou n'a affecté aucune ligne) : on NE retire PAS
            // le claim du cache, pour éviter que ce chunk ne devienne "libre" en apparence tout
            // en restant possédé en base (ce qui rendait tout re-claim impossible pour toujours).
            plugin.getLogger().warning("Échec de la suppression en base du claim " + world + ";" + chunkX + ";" + chunkZ
                    + " (propriétaire : " + claim.getOwnerName() + "). Le claim n'a PAS été retiré, réessayez.");
            return UnclaimResult.ERROR;
        }

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

        ClaimData claim = getClaimResynced(world, chunkX, chunkZ);
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

        ClaimData claim = getClaimResynced(world, chunkX, chunkZ);
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
        ClaimData claim = getClaimResynced(world, chunkX, chunkZ);
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

    /**
     * Copie de tous les claims actuellement en cache (lecture seule), utilisée par le
     * dashboard web (voir fr.crewcmoi.web) pour lister l'ensemble des claims du serveur.
     */
    public List<ClaimData> getAllClaims() {
        return new java.util.ArrayList<>(claims.values());
    }

    /**
     * Comme getClaim, mais se resynchronise depuis la base si le cache dit "non claim" :
     * si la base contient bel et bien un claim à cet endroit (cache désynchronisé suite à
     * un échec silencieux d'une écriture précédente, un /reload, etc.), le cache est
     * réparé et le vrai claim est retourné au lieu de faussement répondre "non claim".
     * Utilisé par toutes les actions déclenchées par une commande (/claim unclaim, trust,
     * settings, sell, buy...), qui sont peu fréquentes : le coût d'une lecture DB de secours
     * y est négligeable, contrairement aux vérifications de protection à chaque bloc cassé.
     */
    private ClaimData getClaimResynced(String world, int chunkX, int chunkZ) {
        ClaimData cached = getClaim(world, chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        ClaimData fromDb = databaseManager.getClaim(world, chunkX, chunkZ);
        if (fromDb != null) {
            claims.put(key(world, chunkX, chunkZ), fromDb);
        }
        return fromDb;
    }

    public ClaimData getClaim(Chunk chunk) {
        return getClaim(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    /**
     * Version publique resynchronisée de getClaim(Chunk), pour les commandes (/claim info,
     * /claim settings) qui doivent afficher le vrai claim même en cas de désync cache/DB.
     */
    public ClaimData getClaimResynced(Chunk chunk) {
        return getClaimResynced(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
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

    /**
     * Nombre de claims supplémentaires déjà achetés par ce joueur (mis en cache après
     * une première lecture en base, pour éviter une requête à chaque /claim).
     */
    public int getExtraClaims(UUID playerUuid) {
        return extraClaims.computeIfAbsent(playerUuid, databaseManager::getExtraClaims);
    }

    /**
     * Nombre maximum de claims que ce joueur peut posséder : quota gratuit de la config
     * + claims supplémentaires achetés via /claim shop.
     */
    public int getMaxClaims(Player player) {
        int base = plugin.getConfig().getInt("claims.max-per-player", 0);
        return base + getExtraClaims(player.getUniqueId());
    }

    /**
     * Prix du prochain claim supplémentaire que ce joueur pourrait acheter (augmente à
     * chaque achat selon claims.shop.price-increment).
     */
    public double getNextShopPrice(Player player) {
        double base = plugin.getConfig().getDouble("claims.shop.base-price", 5000);
        double increment = plugin.getConfig().getDouble("claims.shop.price-increment", 2500);
        return base + increment * getExtraClaims(player.getUniqueId());
    }

    /**
     * Tente d'acheter un claim supplémentaire pour ce joueur (retire l'argent, incrémente
     * son quota de claims). Retourne le résultat de l'achat.
     */
    public ShopResult buyExtraClaim(Player player) {
        int maxExtra = plugin.getConfig().getInt("claims.shop.max-extra", 10);
        int current = getExtraClaims(player.getUniqueId());
        if (current >= maxExtra) {
            return ShopResult.LIMIT_REACHED;
        }

        double price = getNextShopPrice(player);
        if (!plugin.getEconomyManager().has(player.getUniqueId(), price)) {
            return ShopResult.NOT_ENOUGH_MONEY;
        }
        if (!plugin.getEconomyManager().withdraw(player.getUniqueId(), price)) {
            return ShopResult.NOT_ENOUGH_MONEY;
        }

        int updated = current + 1;
        databaseManager.setExtraClaims(player.getUniqueId(), updated);
        extraClaims.put(player.getUniqueId(), updated);
        return ShopResult.SUCCESS;
    }

    /**
     * Retourne tous les claims dont le chunk se trouve dans le rayon (en chunks) donné
     * autour de la position indiquée, utilisé par /claim see.
     */
    public List<ClaimData> getNearbyClaims(Location center, int chunkRadius) {
        if (center == null || center.getWorld() == null) {
            return java.util.Collections.emptyList();
        }
        String world = center.getWorld().getName();
        int centerX = center.getBlockX() >> 4;
        int centerZ = center.getBlockZ() >> 4;

        List<ClaimData> nearby = new java.util.ArrayList<>();
        for (ClaimData claim : claims.values()) {
            if (!claim.getWorld().equals(world)) {
                continue;
            }
            if (Math.abs(claim.getChunkX() - centerX) <= chunkRadius
                    && Math.abs(claim.getChunkZ() - centerZ) <= chunkRadius) {
                nearby.add(claim);
            }
        }
        return nearby;
    }

    /**
     * Met en vente le claim du chunk où se trouve le joueur, pour le prix donné (si le
     * joueur en est le propriétaire).
     */
    public SellResult sellClaim(Player player, double price) {
        if (!Double.isFinite(price) || price <= 0) {
            return SellResult.INVALID_PRICE;
        }

        Chunk chunk = player.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        ClaimData claim = getClaimResynced(world, chunkX, chunkZ);
        if (claim == null) {
            return SellResult.NOT_CLAIMED;
        }
        if (!claim.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("crew.claim.admin")) {
            return SellResult.NOT_OWNER;
        }

        databaseManager.setClaimSalePrice(world, chunkX, chunkZ, price);
        claims.put(key(world, chunkX, chunkZ), new ClaimData(world, chunkX, chunkZ,
                claim.getOwnerUuid(), claim.getOwnerName(), claim.getTrusted(), claim.getFlags(), price));
        return SellResult.SUCCESS;
    }

    /**
     * Retire la mise en vente du claim du chunk où se trouve le joueur (si le joueur en
     * est le propriétaire et que le claim est actuellement à vendre).
     */
    public SellResult cancelSale(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        ClaimData claim = getClaimResynced(world, chunkX, chunkZ);
        if (claim == null) {
            return SellResult.NOT_CLAIMED;
        }
        if (!claim.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("crew.claim.admin")) {
            return SellResult.NOT_OWNER;
        }
        if (!claim.isForSale()) {
            return SellResult.NOT_FOR_SALE;
        }

        databaseManager.setClaimSalePrice(world, chunkX, chunkZ, -1);
        claims.put(key(world, chunkX, chunkZ), new ClaimData(world, chunkX, chunkZ,
                claim.getOwnerUuid(), claim.getOwnerName(), claim.getTrusted(), claim.getFlags(), -1));
        return SellResult.SUCCESS;
    }

    /**
     * Tente d'acheter le claim du chunk où se trouve le joueur, s'il est actuellement mis
     * en vente par son propriétaire. Transfère la propriété, débite l'acheteur et crédite
     * l'ancien propriétaire.
     */
    public BuyResult buyClaim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        ClaimData claim = getClaimResynced(world, chunkX, chunkZ);
        if (claim == null) {
            return BuyResult.NOT_CLAIMED;
        }
        if (!claim.isForSale()) {
            return BuyResult.NOT_FOR_SALE;
        }
        if (claim.getOwnerUuid().equals(player.getUniqueId())) {
            return BuyResult.OWN_CLAIM;
        }

        double price = claim.getSalePrice();
        if (!plugin.getEconomyManager().has(player.getUniqueId(), price)) {
            return BuyResult.NOT_ENOUGH_MONEY;
        }

        int max = getMaxClaims(player);
        if (max > 0 && !player.hasPermission("crew.claim.bypasslimit")) {
            long owned = claims.values().stream()
                    .filter(c -> c.getOwnerUuid().equals(player.getUniqueId()))
                    .count();
            if (owned >= max) {
                return BuyResult.LIMIT_REACHED;
            }
        }

        if (!plugin.getEconomyManager().withdraw(player.getUniqueId(), price)) {
            return BuyResult.NOT_ENOUGH_MONEY;
        }
        plugin.getEconomyManager().deposit(claim.getOwnerUuid(), price);

        databaseManager.transferClaim(world, chunkX, chunkZ, player.getUniqueId(), player.getName());
        claims.put(key(world, chunkX, chunkZ), new ClaimData(world, chunkX, chunkZ,
                player.getUniqueId(), player.getName(), java.util.Collections.emptySet(), claim.getFlags(), -1));
        return BuyResult.SUCCESS;
    }

    /**
     * Retourne tous les claims actuellement mis en vente par leur propriétaire, triés par
     * prix croissant, utilisé par la GUI /claim ah (hôtel des ventes des claims).
     */
    public List<ClaimData> getClaimsForSale() {
        List<ClaimData> forSale = new java.util.ArrayList<>();
        for (ClaimData claim : claims.values()) {
            if (claim.isForSale()) {
                forSale.add(claim);
            }
        }
        forSale.sort(java.util.Comparator.comparingDouble(ClaimData::getSalePrice));
        return forSale;
    }

    /**
     * Tente d'acheter un claim précis mis en vente (utilisé par la GUI /claim ah, où
     * l'acheteur n'est pas forcément sur place, contrairement à /claim buy qui n'agit
     * que sur le chunk où se trouve le joueur).
     */
    public BuyResult buyClaimAt(Player player, String world, int chunkX, int chunkZ) {
        ClaimData claim = getClaimResynced(world, chunkX, chunkZ);
        if (claim == null) {
            return BuyResult.NOT_CLAIMED;
        }
        if (!claim.isForSale()) {
            return BuyResult.NOT_FOR_SALE;
        }
        if (claim.getOwnerUuid().equals(player.getUniqueId())) {
            return BuyResult.OWN_CLAIM;
        }

        double price = claim.getSalePrice();
        if (!plugin.getEconomyManager().has(player.getUniqueId(), price)) {
            return BuyResult.NOT_ENOUGH_MONEY;
        }

        int max = getMaxClaims(player);
        if (max > 0 && !player.hasPermission("crew.claim.bypasslimit")) {
            long owned = claims.values().stream()
                    .filter(c -> c.getOwnerUuid().equals(player.getUniqueId()))
                    .count();
            if (owned >= max) {
                return BuyResult.LIMIT_REACHED;
            }
        }

        if (!plugin.getEconomyManager().withdraw(player.getUniqueId(), price)) {
            return BuyResult.NOT_ENOUGH_MONEY;
        }
        plugin.getEconomyManager().deposit(claim.getOwnerUuid(), price);

        databaseManager.transferClaim(world, chunkX, chunkZ, player.getUniqueId(), player.getName());
        claims.put(key(world, chunkX, chunkZ), new ClaimData(world, chunkX, chunkZ,
                player.getUniqueId(), player.getName(), java.util.Collections.emptySet(), claim.getFlags(), -1));
        return BuyResult.SUCCESS;
    }

    public enum ClaimResult {
        SUCCESS, ALREADY_CLAIMED, LIMIT_REACHED
    }

    public enum SellResult {
        SUCCESS, INVALID_PRICE, NOT_CLAIMED, NOT_OWNER, NOT_FOR_SALE
    }

    public enum BuyResult {
        SUCCESS, NOT_CLAIMED, NOT_FOR_SALE, OWN_CLAIM, NOT_ENOUGH_MONEY, LIMIT_REACHED
    }

    public enum UnclaimResult {
        SUCCESS, NOT_CLAIMED, NOT_OWNER, ERROR
    }

    public enum TrustResult {
        SUCCESS, NOT_CLAIMED, NOT_OWNER
    }

    public enum ShopResult {
        SUCCESS, NOT_ENOUGH_MONEY, LIMIT_REACHED
    }
}
