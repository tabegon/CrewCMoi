package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.auction.AuctionItem;
import fr.crewcmoi.database.BountyEntry;
import fr.crewcmoi.database.BountyTarget;
import fr.crewcmoi.database.ClaimData;
import fr.crewcmoi.database.ClaimFlag;
import fr.crewcmoi.database.ClaimPermission;
import fr.crewcmoi.database.HomeData;
import fr.crewcmoi.database.PlayerData;
import fr.crewcmoi.database.TeamData;
import fr.crewcmoi.utils.ItemSerialization;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
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
                "created_at INTEGER NOT NULL," +
                "reason TEXT," +
                "approved INTEGER NOT NULL DEFAULT 1" +
                ");";

        String serverBountyCountSql = "CREATE TABLE IF NOT EXISTS server_bounty_count (" +
                "player_uuid TEXT NOT NULL," +
                "day TEXT NOT NULL," +
                "count INTEGER NOT NULL DEFAULT 0," +
                "PRIMARY KEY (player_uuid, day)" +
                ");";

        String homesSql = "CREATE TABLE IF NOT EXISTS homes (" +
                "player_uuid TEXT PRIMARY KEY," +
                "world TEXT NOT NULL," +
                "x REAL NOT NULL," +
                "y REAL NOT NULL," +
                "z REAL NOT NULL," +
                "yaw REAL NOT NULL," +
                "pitch REAL NOT NULL" +
                ");";

        String claimsSql = "CREATE TABLE IF NOT EXISTS claims (" +
                "world TEXT NOT NULL," +
                "chunk_x INTEGER NOT NULL," +
                "chunk_z INTEGER NOT NULL," +
                "owner_uuid TEXT NOT NULL," +
                "owner_name TEXT NOT NULL," +
                "trusted TEXT NOT NULL DEFAULT ''," +
                "flags TEXT NOT NULL DEFAULT ''," +
                "sale_price REAL NOT NULL DEFAULT -1," +
                "PRIMARY KEY (world, chunk_x, chunk_z)" +
                ");";

        String claimBonusSql = "CREATE TABLE IF NOT EXISTS claim_bonus (" +
                "player_uuid TEXT PRIMARY KEY," +
                "extra INTEGER NOT NULL DEFAULT 0" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(teamsSql);
            statement.execute(teamMembersSql);
            statement.execute(bountiesSql);
            statement.execute(serverBountyCountSql);
            statement.execute(homesSql);
            statement.execute(claimsSql);
            statement.execute(claimBonusSql);
            // Migration : ajoute la colonne "flags" si la table "claims" existait déjà
            // depuis une version antérieure du plugin (avant le système de règles).
            try {
                statement.execute("ALTER TABLE claims ADD COLUMN flags TEXT NOT NULL DEFAULT '';");
            } catch (SQLException ignored) {
                // La colonne existe déjà, rien à faire.
            }
            // Migration : ajoute la colonne "sale_price" si la table "claims" existait déjà
            // depuis une version antérieure du plugin (avant /claim sell et /claim buy).
            try {
                statement.execute("ALTER TABLE claims ADD COLUMN sale_price REAL NOT NULL DEFAULT -1;");
            } catch (SQLException ignored) {
                // La colonne existe déjà, rien à faire.
            }
            // Migration : ajoute la colonne "reason" si la table "bounties" existait déjà
            // depuis une version antérieure du plugin (avant /bounty add <joueur> <montant> <raison>).
            try {
                statement.execute("ALTER TABLE bounties ADD COLUMN reason TEXT;");
            } catch (SQLException ignored) {
                // La colonne existe déjà, rien à faire.
            }
            // Migration : ajoute la colonne "approved" (défaut 1 = valide) si la table
            // "bounties" existait déjà, depuis avant le système de validation /bounty review.
            try {
                statement.execute("ALTER TABLE bounties ADD COLUMN approved INTEGER NOT NULL DEFAULT 1;");
            } catch (SQLException ignored) {
                // La colonne existe déjà, rien à faire.
            }
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
    public void addBounty(UUID targetUuid, String targetName, UUID contributorUuid, String contributorName, double amount, String reason) {
        // Une prime avec une raison fournie par un joueur doit être validée par un admin
        // (/bounty review) avant de compter comme "légitime" (voir BountyManager#claimBounty) :
        // elle démarre donc non-approuvée. Une prime sans raison (ou attribuée par le serveur,
        // contributorUuid == null) reste valide immédiatement, comme avant ce système.
        boolean needsReview = contributorUuid != null && reason != null && !reason.isBlank();
        String sql = "INSERT INTO bounties (target_uuid, target_name, contributor_uuid, contributor_name, amount, created_at, reason, approved) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
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
            if (reason != null && !reason.isBlank()) {
                ps.setString(7, reason);
            } else {
                ps.setNull(7, Types.VARCHAR);
            }
            ps.setInt(8, needsReview ? 0 : 1);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de l'ajout d'une prime sur " + targetName, e);
        }
    }

    @Override
    public BountyEntry getBountyEntry(int entryId) {
        String sql = "SELECT * FROM bounties WHERE id = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, entryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readBounty(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération de la prime " + entryId, e);
        }
        return null;
    }

    @Override
    public List<BountyEntry> getPendingReasonedBounties() {
        List<BountyEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM bounties WHERE reason IS NOT NULL AND approved = 0 ORDER BY created_at ASC;";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(readBounty(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération des primes en attente de validation.", e);
        }
        return list;
    }

    @Override
    public void setBountyApproved(int entryId, boolean approved) {
        String sql = "UPDATE bounties SET approved = ? WHERE id = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, approved ? 1 : 0);
            ps.setInt(2, entryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la mise à jour de la prime " + entryId, e);
        }
    }

    @Override
    public void deleteBountyEntry(int entryId) {
        String sql = "DELETE FROM bounties WHERE id = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, entryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la suppression de la prime " + entryId, e);
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
    public void clearPlayerBounties(UUID targetUuid) {
        // On conserve les lignes où contributor_uuid EST NULL : ce sont les contributions
        // attribuées par le serveur, qui doivent persister à travers la mort du joueur.
        String sql = "DELETE FROM bounties WHERE target_uuid = ? AND contributor_uuid IS NOT NULL;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la suppression des primes joueurs de " + targetUuid, e);
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

    @Override
    public void setHome(UUID playerUuid, String world, double x, double y, double z, float yaw, float pitch) {
        String sql = "INSERT INTO homes (player_uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET world = excluded.world, x = excluded.x, " +
                "y = excluded.y, z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, world);
            ps.setDouble(3, x);
            ps.setDouble(4, y);
            ps.setDouble(5, z);
            ps.setFloat(6, yaw);
            ps.setFloat(7, pitch);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la définition du home de " + playerUuid, e);
        }
    }

    @Override
    public HomeData getHome(UUID playerUuid) {
        String sql = "SELECT * FROM homes WHERE player_uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new HomeData(
                            playerUuid,
                            rs.getString("world"),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la lecture du home de " + playerUuid, e);
        }
        return null;
    }

    // ===================== CLAIMS =====================

    @Override
    public boolean createClaim(String world, int chunkX, int chunkZ, UUID ownerUuid, String ownerName) {
        if (getClaim(world, chunkX, chunkZ) != null) {
            return false;
        }
        String sql = "INSERT INTO claims (world, chunk_x, chunk_z, owner_uuid, owner_name, trusted) " +
                "VALUES (?, ?, ?, ?, ?, '');";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.setString(4, ownerUuid.toString());
            ps.setString(5, ownerName);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la création du claim.", e);
            return false;
        }
    }

    @Override
    public boolean removeClaim(String world, int chunkX, int chunkZ) {
        String sql = "DELETE FROM claims WHERE world = ? AND chunk_x = ? AND chunk_z = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            // On vérifie le nombre de lignes réellement supprimées : si 0, la ligne n'existait
            // déjà plus (rien à faire, ce n'est pas une erreur) ou la suppression n'a pas eu
            // l'effet attendu. Dans les deux cas, l'appelant doit pouvoir le distinguer d'un
            // vrai succès pour éviter que le cache mémoire ne se désynchronise de la base
            // (symptôme observé : un chunk "unclaim" redevient éternellement impossible à
            // re-claim, car il reste en réalité toujours présent en base).
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la suppression du claim.", e);
            return false;
        }
    }

    @Override
    public ClaimData getClaim(String world, int chunkX, int chunkZ) {
        String sql = "SELECT * FROM claims WHERE world = ? AND chunk_x = ? AND chunk_z = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readClaim(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la lecture du claim.", e);
        }
        return null;
    }

    @Override
    public List<ClaimData> getClaimsByOwner(UUID ownerUuid) {
        List<ClaimData> claims = new ArrayList<>();
        String sql = "SELECT * FROM claims WHERE owner_uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    claims.add(readClaim(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la lecture des claims de " + ownerUuid, e);
        }
        return claims;
    }

    @Override
    public List<ClaimData> getAllClaims() {
        List<ClaimData> claims = new ArrayList<>();
        String sql = "SELECT * FROM claims;";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                claims.add(readClaim(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la lecture de tous les claims.", e);
        }
        return claims;
    }

    @Override
    public void addTrusted(String world, int chunkX, int chunkZ, UUID trustedUuid) {
        ClaimData claim = getClaim(world, chunkX, chunkZ);
        if (claim == null) {
            return;
        }
        java.util.Set<UUID> trusted = new java.util.LinkedHashSet<>(claim.getTrusted());
        trusted.add(trustedUuid);
        updateTrusted(world, chunkX, chunkZ, trusted);
    }

    @Override
    public void removeTrusted(String world, int chunkX, int chunkZ, UUID trustedUuid) {
        ClaimData claim = getClaim(world, chunkX, chunkZ);
        if (claim == null) {
            return;
        }
        java.util.Set<UUID> trusted = new java.util.LinkedHashSet<>(claim.getTrusted());
        trusted.remove(trustedUuid);
        updateTrusted(world, chunkX, chunkZ, trusted);
    }

    private void updateTrusted(String world, int chunkX, int chunkZ, java.util.Set<UUID> trusted) {
        String serialized = String.join(",", trusted.stream().map(UUID::toString).toArray(String[]::new));
        String sql = "UPDATE claims SET trusted = ? WHERE world = ? AND chunk_x = ? AND chunk_z = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, serialized);
            ps.setString(2, world);
            ps.setInt(3, chunkX);
            ps.setInt(4, chunkZ);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la mise à jour des joueurs de confiance du claim.", e);
        }
    }

    @Override
    public void setClaimFlag(String world, int chunkX, int chunkZ, ClaimFlag flag, ClaimPermission permission) {
        ClaimData claim = getClaim(world, chunkX, chunkZ);
        if (claim == null) {
            return;
        }
        Map<ClaimFlag, ClaimPermission> flags = new EnumMap<>(claim.getFlags());
        flags.put(flag, permission);
        String serialized = serializeFlags(flags);
        String sql = "UPDATE claims SET flags = ? WHERE world = ? AND chunk_x = ? AND chunk_z = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, serialized);
            ps.setString(2, world);
            ps.setInt(3, chunkX);
            ps.setInt(4, chunkZ);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la mise à jour des règles du claim.", e);
        }
    }

    private String serializeFlags(Map<ClaimFlag, ClaimPermission> flags) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ClaimFlag, ClaimPermission> entry : flags.entrySet()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(entry.getKey().name()).append(':').append(entry.getValue().name());
        }
        return sb.toString();
    }

    private Map<ClaimFlag, ClaimPermission> deserializeFlags(String raw) {
        Map<ClaimFlag, ClaimPermission> flags = ClaimData.defaultFlags();
        if (raw == null || raw.isEmpty()) {
            return flags;
        }
        for (String part : raw.split(",")) {
            String[] kv = part.split(":");
            if (kv.length != 2) {
                continue;
            }
            try {
                flags.put(ClaimFlag.valueOf(kv[0]), ClaimPermission.valueOf(kv[1]));
            } catch (IllegalArgumentException ignored) {
                // Valeur inconnue (ex: ancienne version), on garde la valeur par défaut.
            }
        }
        return flags;
    }

    @Override
    public int getExtraClaims(UUID playerUuid) {
        String sql = "SELECT extra FROM claim_bonus WHERE player_uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("extra");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la lecture des claims bonus de " + playerUuid, e);
        }
        return 0;
    }

    @Override
    public void setExtraClaims(UUID playerUuid, int amount) {
        String sql = "INSERT INTO claim_bonus (player_uuid, extra) VALUES (?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET extra = excluded.extra;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la mise à jour des claims bonus de " + playerUuid, e);
        }
    }

    @Override
    public void setClaimSalePrice(String world, int chunkX, int chunkZ, double price) {
        String sql = "UPDATE claims SET sale_price = ? WHERE world = ? AND chunk_x = ? AND chunk_z = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, price);
            ps.setString(2, world);
            ps.setInt(3, chunkX);
            ps.setInt(4, chunkZ);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la mise en vente du claim.", e);
        }
    }

    @Override
    public void transferClaim(String world, int chunkX, int chunkZ, UUID newOwnerUuid, String newOwnerName) {
        String sql = "UPDATE claims SET owner_uuid = ?, owner_name = ?, trusted = '', sale_price = -1 " +
                "WHERE world = ? AND chunk_x = ? AND chunk_z = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newOwnerUuid.toString());
            ps.setString(2, newOwnerName);
            ps.setString(3, world);
            ps.setInt(4, chunkX);
            ps.setInt(5, chunkZ);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors du transfert de propriété du claim.", e);
        }
    }

    private ClaimData readClaim(ResultSet rs) throws SQLException {
        String world = rs.getString("world");
        int chunkX = rs.getInt("chunk_x");
        int chunkZ = rs.getInt("chunk_z");
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        String ownerName = rs.getString("owner_name");
        String trustedRaw = rs.getString("trusted");
        java.util.Set<UUID> trusted = new java.util.LinkedHashSet<>();
        if (trustedRaw != null && !trustedRaw.isEmpty()) {
            for (String part : trustedRaw.split(",")) {
                if (!part.isEmpty()) {
                    trusted.add(UUID.fromString(part));
                }
            }
        }
        Map<ClaimFlag, ClaimPermission> flags = deserializeFlags(rs.getString("flags"));
        double salePrice = rs.getDouble("sale_price");
        return new ClaimData(world, chunkX, chunkZ, ownerUuid, ownerName, trusted, flags, salePrice);
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
        String reason = rs.getString("reason");
        boolean approved = rs.getInt("approved") != 0;
        return new BountyEntry(id, targetUuid, targetName, contributorUuid, contributorName, amount, createdAt, reason, approved);
    }
}
