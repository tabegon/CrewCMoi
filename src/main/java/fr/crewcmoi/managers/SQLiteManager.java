package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.PlayerData;

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
}
