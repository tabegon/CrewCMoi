package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.auction.AuctionItem;
import fr.crewcmoi.database.BountyEntry;
import fr.crewcmoi.database.BountyTarget;
import fr.crewcmoi.database.PlayerData;
import fr.crewcmoi.database.TeamData;
import fr.crewcmoi.util.ItemSerialization;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        String teamsSql = "CREATE TABLE IF NOT EXISTS teams (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE COLLATE NOCASE," +
                "owner_uuid TEXT NOT NULL" +
                ");";

        String teamMembersSql = "CREATE TABLE IF NOT EXISTS team_members (" +
                "team_id INTEGER NOT NULL," +
                "player_uuid TEXT PRIMARY KEY," +
                "player_name TEXT NOT NULL" +
                ");";

        String bountiesSql = "CREATE TABLE IF NOT EXISTS bounties (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "target_uuid TEXT NOT NULL," +
                "target_name TEXT NOT NULL," +
                "contributor_uuid TEXT," +
                "contributor_name TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "created_at INTEGER NOT NULL" +
                ");";

        String serverBountyCountSql = "CREATE TABLE IF NOT EXISTS server_bounty_count (" +
                "player_uuid TEXT NOT NULL," +
                "day TEXT NOT NULL," +
                "count INTEGER NOT NULL DEFAULT 0," +
                "PRIMARY KEY (player_uuid, day)" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(teamsSql);
            statement.execute(teamMembersSql);
            statement.execute(bountiesSql);
            statement.execute(serverBountyCountSql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la création des tables team/bounty.", e);
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

    // ===================== TEAMS =====================

    @Override
    public int createTeam(String name, UUID ownerUuid, String ownerName) {
        String sql = "INSERT INTO teams (name, owner_uuid) VALUES (?, ?);";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, ownerUuid.toString());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int teamId = keys.getInt(1);
                    if (addTeamMember(teamId, ownerUuid, ownerName)) {
                        return teamId;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la création de l'équipe " + name, e);
        }
        return -1;
    }

    @Override
    public boolean addTeamMember(int teamId, UUID playerUuid, String playerName) {
        String sql = "INSERT OR REPLACE INTO team_members (team_id, player_uuid, player_name) VALUES (?, ?, ?);";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setString(2, playerUuid.toString());
            ps.setString(3, playerName);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de l'ajout du membre " + playerName + " à l'équipe " + teamId, e);
            return false;
        }
    }

    @Override
    public void removeTeamMember(UUID playerUuid) {
        String sql = "DELETE FROM team_members WHERE player_uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors du retrait du membre " + playerUuid, e);
        }
    }

    @Override
    public void deleteTeam(int teamId) {
        String deleteMembersSql = "DELETE FROM team_members WHERE team_id = ?;";
        String deleteTeamSql = "DELETE FROM teams WHERE id = ?;";
        try (PreparedStatement psMembers = connection.prepareStatement(deleteMembersSql);
             PreparedStatement psTeam = connection.prepareStatement(deleteTeamSql)) {
            psMembers.setInt(1, teamId);
            psMembers.executeUpdate();
            psTeam.setInt(1, teamId);
            psTeam.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la suppression de l'équipe " + teamId, e);
        }
    }

    @Override
    public TeamData getTeamByPlayer(UUID playerUuid) {
        String sql = "SELECT team_id FROM team_members WHERE player_uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return getTeamById(rs.getInt("team_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération de l'équipe du joueur " + playerUuid, e);
        }
        return null;
    }

    @Override
    public TeamData getTeamByName(String name) {
        String sql = "SELECT id FROM teams WHERE name = ? COLLATE NOCASE;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return getTeamById(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération de l'équipe " + name, e);
        }
        return null;
    }

    private TeamData getTeamById(int teamId) {
        String teamSql = "SELECT * FROM teams WHERE id = ?;";
        try (PreparedStatement ps = connection.prepareStatement(teamSql)) {
            ps.setInt(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String name = rs.getString("name");
                UUID owner = UUID.fromString(rs.getString("owner_uuid"));

                Map<UUID, String> members = new LinkedHashMap<>();
                String membersSql = "SELECT player_uuid, player_name FROM team_members WHERE team_id = ?;";
                try (PreparedStatement psMembers = connection.prepareStatement(membersSql)) {
                    psMembers.setInt(1, teamId);
                    try (ResultSet rsMembers = psMembers.executeQuery()) {
                        while (rsMembers.next()) {
                            members.put(UUID.fromString(rsMembers.getString("player_uuid")), rsMembers.getString("player_name"));
                        }
                    }
                }

                return new TeamData(teamId, name, owner, members);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération de l'équipe " + teamId, e);
            return null;
        }
    }

    // ===================== BOUNTIES =====================

    @Override
    public void addBounty(UUID targetUuid, String targetName, UUID contributorUuid, String contributorName, double amount) {
        String sql = "INSERT INTO bounties (target_uuid, target_name, contributor_uuid, contributor_name, amount, created_at) VALUES (?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetUuid.toString());
            ps.setString(2, targetName);
            if (contributorUuid != null) {
                ps.setString(3, contributorUuid.toString());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, contributorName);
            ps.setDouble(5, amount);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de l'ajout d'une prime sur " + targetName, e);
        }
    }

    @Override
    public List<BountyEntry> getBounties(UUID targetUuid) {
        List<BountyEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM bounties WHERE target_uuid = ? ORDER BY created_at ASC;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(readBounty(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération des primes de " + targetUuid, e);
        }
        return list;
    }

    @Override
    public List<BountyTarget> getBountyTargets() {
        List<BountyTarget> list = new ArrayList<>();
        String sql = "SELECT target_uuid, target_name, SUM(amount) AS total, COUNT(*) AS nb " +
                "FROM bounties GROUP BY target_uuid ORDER BY total DESC;";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new BountyTarget(
                        UUID.fromString(rs.getString("target_uuid")),
                        rs.getString("target_name"),
                        rs.getDouble("total"),
                        rs.getInt("nb")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération de la liste des primes.", e);
        }
        return list;
    }

    @Override
    public void clearBounties(UUID targetUuid) {
        String sql = "DELETE FROM bounties WHERE target_uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la suppression des primes de " + targetUuid, e);
        }
    }

    @Override
    public int getServerBountyCountToday(UUID playerUuid) {
        String sql = "SELECT count FROM server_bounty_count WHERE player_uuid = ? AND day = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, today());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la lecture du compteur de prime serveur de " + playerUuid, e);
        }
        return 0;
    }

    @Override
    public void incrementServerBountyCount(UUID playerUuid) {
        String sql = "INSERT INTO server_bounty_count (player_uuid, day, count) VALUES (?, ?, 1) " +
                "ON CONFLICT(player_uuid, day) DO UPDATE SET count = count + 1;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, today());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de l'incrémentation du compteur de prime serveur de " + playerUuid, e);
        }
    }

    private String today() {
        return LocalDate.now(ZoneId.systemDefault()).toString();
    }

    private BountyEntry readBounty(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
        String targetName = rs.getString("target_name");
        String contributorUuidStr = rs.getString("contributor_uuid");
        UUID contributorUuid = contributorUuidStr != null ? UUID.fromString(contributorUuidStr) : null;
        String contributorName = rs.getString("contributor_name");
        double amount = rs.getDouble("amount");
        long createdAt = rs.getLong("created_at");
        return new BountyEntry(id, targetUuid, targetName, contributorUuid, contributorName, amount, createdAt);
    }
}
