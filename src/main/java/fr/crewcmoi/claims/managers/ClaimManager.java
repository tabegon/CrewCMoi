package fr.crewcmoi.claims.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.database.ClaimFlag;
import fr.crewcmoi.claims.database.ClaimPermission;
import fr.crewcmoi.other.managers.DatabaseManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {

    private final Main plugin;
    private final DatabaseManager databaseManager;

    private final Map<String, ClaimData> claims = new ConcurrentHashMap<>();

    private final Map<UUID, Integer> extraClaims = new ConcurrentHashMap<>();
    private final File zonesFile;
    private final Map<UUID, Location[]> zoneSelections = new ConcurrentHashMap<>();

    public ClaimManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.zonesFile = new File(plugin.getDataFolder(), "claim-zones.yml");
        if (!zonesFile.exists()) {
            try { plugin.getDataFolder().mkdirs(); zonesFile.createNewFile(); } catch (IOException exception) {
                plugin.getLogger().warning("Impossible de créer claim-zones.yml : " + exception.getMessage());
            }
        }
    }

    public void loadAll() {
        for (ClaimData claim : databaseManager.getAllClaims()) {
            claims.put(key(claim.getWorld(), claim.getChunkX(), claim.getChunkZ()), claim);
        }
        plugin.getLogger().info(claims.size() + " claim(s) chargé(s).");
    }

    private String key(String world, int chunkX, int chunkZ) {
        return world + ";" + chunkX + ";" + chunkZ;
    }

    public void setZoneCorner(Player player, int corner) {
        Location[] corners = zoneSelections.computeIfAbsent(player.getUniqueId(), key -> new Location[2]);
        corners[corner - 1] = player.getLocation().getBlock().getLocation();
    }

    public boolean addBlockedZone(Player player, String name) {
        Location[] corners = zoneSelections.get(player.getUniqueId());
        if (corners == null || corners[0] == null || corners[1] == null
                || corners[0].getWorld() == null || !corners[0].getWorld().equals(corners[1].getWorld())) return false;
        String safeName = name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        if (safeName.isBlank()) return false;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(zonesFile);
        String path = "zones." + safeName;
        yaml.set(path + ".world", corners[0].getWorld().getName());
        yaml.set(path + ".min-x", Math.min(corners[0].getBlockX(), corners[1].getBlockX()));
        yaml.set(path + ".min-y", Math.min(corners[0].getBlockY(), corners[1].getBlockY()));
        yaml.set(path + ".min-z", Math.min(corners[0].getBlockZ(), corners[1].getBlockZ()));
        yaml.set(path + ".max-x", Math.max(corners[0].getBlockX(), corners[1].getBlockX()));
        yaml.set(path + ".max-y", Math.max(corners[0].getBlockY(), corners[1].getBlockY()));
        yaml.set(path + ".max-z", Math.max(corners[0].getBlockZ(), corners[1].getBlockZ()));
        try { yaml.save(zonesFile); zoneSelections.remove(player.getUniqueId()); return true; }
        catch (IOException exception) { plugin.getLogger().warning("Impossible de sauvegarder la zone de claim : " + exception.getMessage()); return false; }
    }

    public boolean removeBlockedZone(String name) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(zonesFile);
        String path = "zones." + name.toLowerCase(java.util.Locale.ROOT);
        if (!yaml.contains(path)) return false;
        yaml.set(path, null);
        try { yaml.save(zonesFile); return true; }
        catch (IOException exception) { plugin.getLogger().warning("Impossible de modifier claim-zones.yml : " + exception.getMessage()); return false; }
    }

    public java.util.Set<String> getBlockedZoneNames() {
        return YamlConfiguration.loadConfiguration(zonesFile).getConfigurationSection("zones") == null
                ? java.util.Collections.emptySet()
                : new java.util.TreeSet<>(YamlConfiguration.loadConfiguration(zonesFile).getConfigurationSection("zones").getKeys(false));
    }

    public boolean isClaimBlocked(Location location) {
        if (location == null || location.getWorld() == null) return false;
        List<Map<?, ?>> configured = plugin.getConfig().getMapList("claims.blocked-zones");
        for (Map<?, ?> zone : configured) if (isInside(location, zone)) return true;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(zonesFile);
        if (yaml.getConfigurationSection("zones") == null) return false;
        for (String name : yaml.getConfigurationSection("zones").getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection zone = yaml.getConfigurationSection("zones." + name);
            if (zone == null) continue;
            int chunkMinX = location.getChunk().getX() * 16;
            int chunkMaxX = chunkMinX + 15;
            int chunkMinZ = location.getChunk().getZ() * 16;
            int chunkMaxZ = chunkMinZ + 15;
            if (location.getWorld().getName().equalsIgnoreCase(zone.getString("world", ""))
                    && overlaps(chunkMinX, chunkMaxX, zone.getInt("min-x"), zone.getInt("max-x"))
                    && overlaps(chunkMinZ, chunkMaxZ, zone.getInt("min-z"), zone.getInt("max-z"))) return true;
        }
        return false;
    }

    private boolean isInside(Location location, Map<?, ?> zone) {
        String world = zone.containsKey("world") ? String.valueOf(zone.get("world")) : "";
        int chunkMinX = location.getChunk().getX() * 16;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = location.getChunk().getZ() * 16;
        int chunkMaxZ = chunkMinZ + 15;
        return location.getWorld().getName().equalsIgnoreCase(world)
                && overlaps(chunkMinX, chunkMaxX, number(zone.get("min-x")), number(zone.get("max-x")))
                && overlaps(chunkMinZ, chunkMaxZ, number(zone.get("min-z")), number(zone.get("max-z")));
    }

    private int number(Object value) { return value instanceof Number n ? n.intValue() : Integer.MIN_VALUE; }
    private boolean overlaps(int aMin, int aMax, int bMin, int bMax) { return bMin <= bMax && aMin <= bMax && aMax >= bMin; }

    public ClaimResult claim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        if (isClaimBlocked(player.getLocation())) return ClaimResult.BLOCKED_ZONE;
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

    
    public UnclaimResult adminUnclaimAt(Player player, String world, int chunkX, int chunkZ) {
        if (!player.hasPermission("crew.claim.admin")) {
            return UnclaimResult.NOT_OWNER;
        }
        ClaimData claim = getClaimResynced(world, chunkX, chunkZ);
        if (claim == null) {
            return UnclaimResult.NOT_CLAIMED;
        }
        boolean removed = databaseManager.removeClaim(world, chunkX, chunkZ);
        if (!removed) {
            plugin.getLogger().warning("Échec de la suppression admin du claim " + world + ";" + chunkX + ";" + chunkZ);
            return UnclaimResult.ERROR;
        }
        claims.remove(key(world, chunkX, chunkZ));
        return UnclaimResult.SUCCESS;
    }

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

            
            plugin.getLogger().warning("Échec de la suppression en base du claim " + world + ";" + chunkX + ";" + chunkZ
                    + " (propriétaire : " + claim.getOwnerName() + "). Le claim n'a PAS été retiré, réessayez.");
            return UnclaimResult.ERROR;
        }

        claims.remove(key(world, chunkX, chunkZ));
        return UnclaimResult.SUCCESS;
    }

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
                claim.getOwnerUuid(), claim.getOwnerName(), trusted, claim.getFlags(), claim.getSalePrice()));
        return TrustResult.SUCCESS;
    }

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
                claim.getOwnerUuid(), claim.getOwnerName(), trusted, claim.getFlags(), claim.getSalePrice()));
        return TrustResult.SUCCESS;
    }

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

    public List<ClaimData> getAllClaims() {
        return new java.util.ArrayList<>(claims.values());
    }

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

    public ClaimData getClaimResynced(Chunk chunk) {
        return getClaimResynced(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public ClaimData getClaim(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return getClaim(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission("crew.claim.bypass")) {
            return true;
        }
        ClaimData claim = getClaim(location);
        return claim == null || claim.canBuild(player.getUniqueId());
    }

    public boolean isAllowed(Player player, Location location, ClaimFlag flag) {
        if (player.hasPermission("crew.claim.bypass")) {
            return true;
        }
        ClaimData claim = getClaim(location);
        return claim == null || claim.isAllowed(player.getUniqueId(), flag);
    }

    public boolean isOpen(Location location, ClaimFlag flag) {
        ClaimData claim = getClaim(location);
        return claim == null || claim.isOpen(flag);
    }

    public int getExtraClaims(UUID playerUuid) {
        return extraClaims.computeIfAbsent(playerUuid, databaseManager::getExtraClaims);
    }

    public int getMaxClaims(Player player) {
        int base = plugin.getConfig().getInt("claims.max-per-player", 0);
        return base + getExtraClaims(player.getUniqueId());
    }

    public double getNextShopPrice(Player player) {
        double base = plugin.getConfig().getDouble("claims.shop.base-price", 5000);
        double increment = plugin.getConfig().getDouble("claims.shop.price-increment", 2500);
        return base + increment * getExtraClaims(player.getUniqueId());
    }

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
        SUCCESS, ALREADY_CLAIMED, LIMIT_REACHED, BLOCKED_ZONE
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
