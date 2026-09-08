package fr.crewcmoi.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.database.ClaimFlag;
import fr.crewcmoi.claims.managers.ClaimManager;
import fr.crewcmoi.economie.auction.AuctionItem;
import fr.crewcmoi.economie.managers.AuctionManager;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.economie.managers.PricesManager;
import fr.crewcmoi.other.database.PlayerData;
import fr.crewcmoi.pvp.database.BountyTarget;
import fr.crewcmoi.pvp.managers.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebDashboardServer {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final PricesManager pricesManager;
    private final ClaimManager claimManager;
    private final BountyManager bountyManager;
    private final AuctionManager auctionManager;
    private final PlayerActivityTracker activityTracker = new PlayerActivityTracker();

    private HttpServer server;
    private ExecutorService executor;
    private File siteFolder;

    public WebDashboardServer(Main plugin, EconomyManager economyManager, PricesManager pricesManager,
                              ClaimManager claimManager, BountyManager bountyManager,
                              AuctionManager auctionManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.pricesManager = pricesManager;
        this.claimManager = claimManager;
        this.bountyManager = bountyManager;
        this.auctionManager = auctionManager;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("web.enabled", true)) {
            plugin.getLogger().info("Dashboard web désactivé (web.enabled: false dans config.yml).");
            return;
        }

        int port = plugin.getConfig().getInt("web.port", 8123);
        String bindAddress = plugin.getConfig().getString("web.bind-address", "0.0.0.0");

        extractSiteFiles();
        Bukkit.getPluginManager().registerEvents(activityTracker, plugin);
        activityTracker.initializeOnlinePlayers();

        try {
            server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de démarrer le dashboard web sur " + bindAddress + ":" + port
                    + " (" + e.getMessage() + "). Vérifie que le port n'est pas déjà utilisé.");
            return;
        }

        executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "CrewCMoi-WebDashboard");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(executor);

        server.createContext("/", this::handleStatic);
        server.createContext("/api/status", wrap(this::handleStatus));
        server.createContext("/api/players", wrap(this::handlePlayers));
        server.createContext("/api/config", wrap(this::handleConfig));
        server.createContext("/api/economy/top", wrap(this::handleEconomyTop));
        server.createContext("/api/economy/prices", wrap(this::handleEconomyPrices));
        server.createContext("/api/claims", wrap(this::handleClaims));
        server.createContext("/api/bounties", wrap(this::handleBounties));

        server.createContext("/api/ah", wrap(this::handleAuctionHouse));
        server.createContext("/api/claim-ah", wrap(this::handleClaimAuctionHouse));

        server.start();
        plugin.getLogger().info("Dashboard web démarré sur http://" + bindAddress + ":" + port
                + " (lecture seule, réseau local uniquement recommandé).");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
    }

    

    private void extractSiteFiles() {
        siteFolder = new File(plugin.getDataFolder(), "site");
        if (!siteFolder.exists()) {
            siteFolder.mkdirs();
        }
        saveIfMissing("site/index.html");
        saveIfMissing("site/style.css");
        saveIfMissing("site/app.js");
    }

    private void saveIfMissing(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (target.exists()) {
            return;
        }
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                plugin.getLogger().warning("Ressource introuvable dans le jar : " + resourcePath);
                return;
            }
            target.getParentFile().mkdirs();
            Files.copy(in, target.toPath());
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'extraire " + resourcePath + " : " + e.getMessage());
        }
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }
            String fileName = path.substring(1);
            if (!fileName.equals("index.html") && !fileName.equals("style.css") && !fileName.equals("app.js")) {
                sendText(exchange, 404, "text/plain", "Not found");
                return;
            }

            File file = new File(siteFolder, fileName);
            if (!file.exists()) {
                sendText(exchange, 404, "text/plain", "Not found");
                return;
            }

            byte[] content = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentTypeFor(fileName));
            exchange.sendResponseHeaders(200, content.length);
            try (var os = exchange.getResponseBody()) {
                os.write(content);
            }
        } finally {
            exchange.close();
        }
    }

    private String contentTypeFor(String fileName) {
        if (fileName.endsWith(".html")) return "text/html; charset=utf-8";
        if (fileName.endsWith(".css")) return "text/css; charset=utf-8";
        if (fileName.endsWith(".js")) return "application/javascript; charset=utf-8";
        return "application/octet-stream";
    }

    

    private interface ApiHandler {
        Object handle(URI uri) throws Exception;
    }

    private HttpHandler wrap(ApiHandler handler) {
        return exchange -> {
            try {
                Object result = handler.handle(exchange.getRequestURI());
                sendJson(exchange, 200, result);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur dashboard web sur " + exchange.getRequestURI() + " : " + e.getMessage());
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "internal_error");
                sendJson(exchange, 500, error);
            } finally {
                exchange.close();
            }
        };
    }

    private Object handleStatus(URI uri) throws Exception {
        return runSync(() -> {
            Map<String, Object> data = new LinkedHashMap<>();
            List<String> onlineNames = new ArrayList<>();
            for (var p : Bukkit.getOnlinePlayers()) {
                onlineNames.add(p.getName());
            }
            data.put("onlinePlayers", onlineNames);
            data.put("onlineCount", onlineNames.size());
            data.put("maxPlayers", Bukkit.getMaxPlayers());
            data.put("serverVersion", Bukkit.getVersion());
            data.put("pluginVersion", plugin.getDescription().getVersion());
            try {
                double[] tps = Bukkit.getTPS();
                if (tps != null && tps.length > 0) {
                    data.put("tps1m", Math.round(tps[0] * 100.0) / 100.0);
                }
            } catch (Throwable ignored) {
            }
            return data;
        });
    }

    private Object handlePlayers(URI uri) throws Exception {
        return runSync(() -> {
            List<Object> result = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            sdf.setTimeZone(TimeZone.getDefault());

            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() == null) {
                    continue;
                }

                UUID uuid = op.getUniqueId();
                Player onlinePlayer = Bukkit.getPlayer(uuid);
                boolean online = onlinePlayer != null && onlinePlayer.isOnline();

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", op.getName());
                row.put("uuid", uuid.toString());
                row.put("online", online);

                double balance = 0.0D;
                try {
                    balance = economyManager.getBalance(uuid);
                } catch (Exception ignored) {
                }
                row.put("money", balance);
                row.put("balance", balance);

                long playTicks = 0L;
                try {
                    playTicks = op.getStatistic(Statistic.PLAY_ONE_MINUTE);
                } catch (IllegalArgumentException ignored) {
                }
                row.put("playTime", formatPlayTime(playTicks / 20L));

                long lastLoginMs = op.getLastLogin();
                row.put("lastLogin", lastLoginMs > 0L ? sdf.format(new Date(lastLoginMs)) : "Inconnu");
                row.put("lastLoginTimestamp", lastLoginMs > 0L ? lastLoginMs : null);

                row.put("lastMessages", activityTracker.getLastMessages(uuid));
                row.put("lastCommands", activityTracker.getLastCommands(uuid));
                row.put("connectionTime", online ? activityTracker.getFormattedSessionDuration(uuid) : "Hors ligne");
                row.put("connectionTimeSeconds", online ? activityTracker.getSessionDurationMillis(uuid) / 1000L : 0L);

                org.bukkit.Location loc = null;

                if (online) {
                    Player p = onlinePlayer;
                    row.put("world", p.getWorld() != null ? p.getWorld().getName() : null);
                    row.put("health", p.getHealth());
                    row.put("level", p.getLevel());
                    row.put("op", p.isOp());

                    loc = p.getLocation();

                    List<Object> inventory = new ArrayList<>();
                    for (var item : p.getInventory().getStorageContents()) {
                        if (item == null || item.getType().isAir()) {
                            inventory.add(null);
                        } else {
                            Map<String, Object> itemData = new LinkedHashMap<>();
                            itemData.put("type", item.getType().name());
                            itemData.put("amount", item.getAmount());
                            inventory.add(itemData);
                        }
                    }
                    row.put("inventory", inventory);
                    row.put("inv", inventory);
                } else {
                    loc = op.getLocation();

                    row.put("world", loc != null && loc.getWorld() != null ? loc.getWorld().getName() : null);
                    row.put("health", null);
                    row.put("level", null);
                    row.put("op", op.isOp());
                    row.put("inventory", new ArrayList<>());
                    row.put("inv", new ArrayList<>());
                }

                if (loc != null && loc.getWorld() != null) {
                    Map<String, Object> coords = new LinkedHashMap<>();
                    coords.put("world", loc.getWorld().getName());
                    coords.put("x", loc.getBlockX());
                    coords.put("y", loc.getBlockY());
                    coords.put("z", loc.getBlockZ());

                    row.put("lastCoords", coords);
                    row.put("coordinates", coords);
                } else {
                    row.put("lastCoords", null);
                    row.put("coordinates", null);
                }

                result.add(row);
            }
            return result;
        });
    }

    private String formatPlayTime(long totalSeconds) {
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private Object handleConfig(URI uri) {
        ConfigurationSection root = plugin.getConfig();
        return JsonUtil.fromConfigSection(root);
    }

    private Object handleEconomyTop(URI uri) {
        int limit = parseIntParam(uri, "limit", 50);
        List<PlayerData> top = economyManager.getTopBalances(limit);
        List<Object> result = new ArrayList<>();
        int rank = 1;
        for (PlayerData data : top) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", rank++);
            entry.put("name", data.getName());
            entry.put("uuid", data.getUuid());
            entry.put("balance", data.getBalance());
            result.add(entry);
        }
        return result;
    }

    private Object handleEconomyPrices(URI uri) {
        Map<Material, Double> prices = pricesManager.getAllPrices();
        List<Object> result = new ArrayList<>();
        for (Map.Entry<Material, Double> entry : prices.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("material", entry.getKey().name());
            row.put("price", entry.getValue());
            result.add(row);
        }
        result.sort((a, b) -> String.valueOf(((Map<?, ?>) a).get("material"))
                .compareTo(String.valueOf(((Map<?, ?>) b).get("material"))));
        return result;
    }

    private Object handleClaims(URI uri) {
        List<Object> result = new ArrayList<>();
        for (ClaimData claim : claimManager.getAllClaims()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("world", claim.getWorld());
            row.put("chunkX", claim.getChunkX());
            row.put("chunkZ", claim.getChunkZ());
            row.put("owner", claim.getOwnerName());
            row.put("ownerUuid", claim.getOwnerUuid());
            row.put("trustedCount", claim.getTrusted().size());
            row.put("forSale", claim.isForSale());
            if (claim.isForSale()) {
                row.put("salePrice", claim.getSalePrice());
            }
            Map<String, Object> flags = new LinkedHashMap<>();
            for (ClaimFlag flag : ClaimFlag.values()) {
                flags.put(flag.name(), claim.getPermission(flag).name());
            }
            row.put("flags", flags);
            result.add(row);
        }
        return result;
    }

    private Object handleBounties(URI uri) {
        List<BountyTarget> targets = bountyManager.getBountyTargets();
        List<Object> result = new ArrayList<>();
        for (BountyTarget target : targets) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("target", target.getTargetName());
            row.put("targetUuid", target.getTargetUuid());
            row.put("totalAmount", target.getTotalAmount());
            row.put("contributorCount", target.getContributorCount());
            result.add(row);
        }
        return result;
    }

    

    

    private Object handleAuctionHouse(URI uri) throws Exception {
        return runSync(() -> {
            Map<String, Object> responseData = new LinkedHashMap<>();
            List<Object> listings = new ArrayList<>();

            if (auctionManager != null) {
                for (AuctionItem item : auctionManager.getItems()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", item.getId());
                    row.put("sellerName", item.getSellerName());
                    row.put("price", item.getPrice());

                    ItemStack stack = item.getItem();
                    if (stack != null && !stack.getType().isAir()) {
                        Map<String, Object> itemData = new LinkedHashMap<>();
                        itemData.put("type", stack.getType().name());
                        itemData.put("amount", stack.getAmount());
                        row.put("item", itemData);
                    }
                    listings.add(row);
                }
            }

            responseData.put("listings", listings);

            responseData.put("lastSale", auctionManager != null ? auctionManager.getLastSaleData() : null);

            return responseData;
        });
    }

    private Object handleClaimAuctionHouse(URI uri) {
        List<Object> result = new ArrayList<>();
        for (ClaimData claim : claimManager.getAllClaims()) {
            if (claim.isForSale()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("world", claim.getWorld());
                row.put("chunkX", claim.getChunkX());
                row.put("chunkZ", claim.getChunkZ());
                row.put("owner", claim.getOwnerName());
                row.put("ownerUuid", claim.getOwnerUuid());
                row.put("price", claim.getSalePrice());
                result.add(row);
            }
        }
        return result;
    }

    

    private int parseIntParam(URI uri, String name, int defaultValue) {
        String query = uri.getRawQuery();
        if (query == null) {
            return defaultValue;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(name)) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private <T> T runSync(java.util.concurrent.Callable<T> task) throws Exception {
        if (Bukkit.isPrimaryThread()) {
            return task.call();
        }
        var future = Bukkit.getScheduler().callSyncMethod(plugin, task);
        return future.get(5, TimeUnit.SECONDS);
    }

    private void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] bytes = JsonUtil.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendText(HttpExchange exchange, int status, String contentType, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
