package fr.crewcmoi.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.database.ClaimFlag;
import fr.crewcmoi.claims.managers.ClaimManager;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.economie.managers.PricesManager;
import fr.crewcmoi.other.database.PlayerData;
import fr.crewcmoi.pvp.database.BountyTarget;
import fr.crewcmoi.pvp.managers.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Petit serveur web embarqué (aucune dépendance externe, {@code com.sun.net.httpserver}
 * fait partie du JDK) qui expose un dashboard admin en lecture seule : réglages
 * courants (config.yml), classement des richesses, prix de vente, claims et primes.
 *
 * Prévu pour un usage local / réseau local uniquement (voir web.bind-address dans
 * config.yml) : aucune donnée n'est modifiable depuis le site, uniquement consultée.
 */
public class WebDashboardServer {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final PricesManager pricesManager;
    private final ClaimManager claimManager;
    private final BountyManager bountyManager;

    private HttpServer server;
    private ExecutorService executor;
    private File siteFolder;

    public WebDashboardServer(Main plugin, EconomyManager economyManager, PricesManager pricesManager,
                               ClaimManager claimManager, BountyManager bountyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.pricesManager = pricesManager;
        this.claimManager = claimManager;
        this.bountyManager = bountyManager;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("web.enabled", true)) {
            plugin.getLogger().info("Dashboard web désactivé (web.enabled: false dans config.yml).");
            return;
        }

        int port = plugin.getConfig().getInt("web.port", 8123);
        String bindAddress = plugin.getConfig().getString("web.bind-address", "0.0.0.0");

        extractSiteFiles();

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
        server.createContext("/api/config", wrap(this::handleConfig));
        server.createContext("/api/economy/top", wrap(this::handleEconomyTop));
        server.createContext("/api/economy/prices", wrap(this::handleEconomyPrices));
        server.createContext("/api/claims", wrap(this::handleClaims));
        server.createContext("/api/bounties", wrap(this::handleBounties));

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

    // ---------------------------------------------------------------------
    // Fichiers statiques du dashboard (extraits une fois depuis les
    // ressources du plugin vers <dataFolder>/site, modifiables ensuite sans
    // recompiler le plugin).
    // ---------------------------------------------------------------------

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
            // Whitelist stricte : seuls ces fichiers connus peuvent être servis, pour
            // éviter tout accès à un fichier arbitraire du disque (path traversal).
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

    // ---------------------------------------------------------------------
    // Endpoints JSON en lecture seule.
    // ---------------------------------------------------------------------

    private interface ApiHandler {
        Object handle(URI uri) throws Exception;
    }

    /**
     * Enrobe un handler d'API : sérialise le retour en JSON, gère les erreurs
     * proprement (500) et pose les en-têtes CORS/JSON communs.
     */
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
        // Bukkit.getOnlinePlayers() doit être appelé depuis le thread principal du serveur.
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
                // getTPS() est une extension Paper : on l'ignore silencieusement si absente.
            }
            return data;
        });
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

    // ---------------------------------------------------------------------
    // Utilitaires.
    // ---------------------------------------------------------------------

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

    /**
     * Exécute une tâche sur le thread principal du serveur et attend son résultat
     * (nécessaire pour certains appels Bukkit qui ne sont pas thread-safe), avec un
     * timeout pour ne jamais bloquer indéfiniment un thread HTTP.
     */
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
