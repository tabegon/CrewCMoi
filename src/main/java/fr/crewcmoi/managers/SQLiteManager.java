package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.auction.AuctionItem;
import fr.crewcmoi.database.PlayerData;
import fr.crewcmoi.util.ItemSerialization;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Implémentation SQLite du DatabaseManager.
 * Stocke les données dans un fichier data.db à la racine du dossier du plugin.
 */
public class SQLiteManager implements DatabaseManager {

    private final Main plugin;
    private Connection connection;

    public SQLiteManager(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File dbFile = new File(plugin.getDataFolder(), "data.db");

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("Connexion à la base SQLite établie.");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de se connecter à la base SQLite.", e);
        }
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Erreur lors de la fermeture de la base de données.", e);
            }
        }
    }

    @Override
    public void init() {
        String sql = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid TEXT PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "balance REAL NOT NULL DEFAULT 0" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la création de la table players.", e);
        }

        String auctionsSql = "CREATE TABLE IF NOT EXISTS auctions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "seller_uuid TEXT NOT NULL," +
                "seller_name TEXT NOT NULL," +
                "item TEXT NOT NULL," +
                "price REAL NOT NULL," +
                "created_at INTEGER NOT NULL" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(auctionsSql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la création de la table auctions.", e);
        }
    }

    @Override
    public PlayerData loadOrCreatePlayer(UUID uuid, String name) {
        PlayerData existing = getPlayer(uuid);
        if (existing != null) {
            // Met à jour le pseudo si celui-ci a changé
            if (!existing.getName().equals(name)) {
                existing.setName(name);
                savePlayer(existing);
            }
            return existing;
        }

        double startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 0.0);
        PlayerData data = new PlayerData(uuid, name, startingBalance);

        String sql = "INSERT INTO players (uuid, name, balance) VALUES (?, ?, ?);";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setDouble(3, startingBalance);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la création du joueur " + name, e);
        }

        return data;
    }

    @Override
    public void savePlayer(PlayerData data) {
        String sql = "UPDATE players SET name = ?, balance = ? WHERE uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, data.getName());
            ps.setDouble(2, data.getBalance());
            ps.setString(3, data.getUuid().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde du joueur " + data.getName(), e);
        }
    }

    @Override
    public PlayerData getPlayer(UUID uuid) {
        String sql = "SELECT * FROM players WHERE uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            rs.getDouble("balance")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération du joueur " + uuid, e);
        }
        return null;
    }

    @Override
    public PlayerData getPlayerByName(String name) {
        String sql = "SELECT * FROM players WHERE LOWER(name) = LOWER(?);";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            rs.getDouble("balance")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération du joueur " + name, e);
        }
        return null;
    }

    @Override
    public List<PlayerData> getTopBalances(int limit) {
        List<PlayerData> list = new ArrayList<>();
        String sql = "SELECT * FROM players ORDER BY balance DESC LIMIT ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlayerData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            rs.getDouble("balance")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération du classement.", e);
        }
        return list;
    }

    @Override
    public int createAuction(UUID sellerUuid, String sellerName, ItemStack item, double price) {
        String sql = "INSERT INTO auctions (seller_uuid, seller_name, item, price, created_at) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sellerUuid.toString());
            ps.setString(2, sellerName);
            ps.setString(3, ItemSerialization.toBase64(item));
            ps.setDouble(4, price);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la création d'une annonce.", e);
        }
        return -1;
    }

    @Override
    public List<AuctionItem> getActiveAuctions() {
        List<AuctionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions ORDER BY created_at DESC;";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AuctionItem auction = readAuction(rs);
                if (auction != null) {
                    list.add(auction);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération des annonces.", e);
        }
        return list;
    }

    @Override
    public AuctionItem getAuction(int id) {
        String sql = "SELECT * FROM auctions WHERE id = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readAuction(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération de l'annonce " + id, e);
        }
        return null;
    }

    @Override
    public void removeAuction(int id) {
        String sql = "DELETE FROM auctions WHERE id = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la suppression de l'annonce " + id, e);
        }
    }

    private AuctionItem readAuction(ResultSet rs) {
        try {
            int id = rs.getInt("id");
            UUID sellerUuid = UUID.fromString(rs.getString("seller_uuid"));
            String sellerName = rs.getString("seller_name");
            ItemStack item = ItemSerialization.fromBase64(rs.getString("item"));
            double price = rs.getDouble("price");
            long createdAt = rs.getLong("created_at");
            return new AuctionItem(id, sellerUuid, sellerName, item, price, createdAt);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Annonce corrompue ignorée.", e);
            return null;
        }
    }
}
