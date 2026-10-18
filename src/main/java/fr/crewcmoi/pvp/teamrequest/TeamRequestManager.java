package fr.crewcmoi.pvp.teamrequest;

import fr.crewcmoi.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import fr.crewcmoi.other.utils.ItemSerialization;
import java.io.IOException;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TeamRequestManager {
    private final Main plugin;
    private Connection connection;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public record Prompt(String type, int requestId, String itemData) {}
    public record ContributionTotal(String name, double money, long items) {}

    public TeamRequestManager(Main plugin) {
        this.plugin = plugin;
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "team-requests.db").getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS team_requests (id INTEGER PRIMARY KEY AUTOINCREMENT, team_id INTEGER NOT NULL, creator_uuid TEXT NOT NULL, type TEXT NOT NULL, material TEXT, item_data TEXT, target REAL NOT NULL, collected REAL NOT NULL DEFAULT 0, active INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL)");
                try { st.executeUpdate("ALTER TABLE team_requests ADD COLUMN item_data TEXT"); } catch (SQLException ignored) {}
                st.executeUpdate("CREATE TABLE IF NOT EXISTS team_request_contributions (request_id INTEGER NOT NULL, team_id INTEGER NOT NULL, player_uuid TEXT NOT NULL, player_name TEXT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, PRIMARY KEY(request_id, player_uuid, type))");
            }
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Impossible d'initialiser les demandes d'équipe : " + e.getMessage());
        }
    }

    public synchronized boolean create(int teamId, UUID creator, String type, String material, String itemData, double target) {
        if (connection == null || target <= 0 || !Double.isFinite(target)) return false;
        try (PreparedStatement check = connection.prepareStatement("SELECT 1 FROM team_requests WHERE team_id=? AND active=1 LIMIT 1")) {
            check.setInt(1, teamId);
            try (ResultSet rs = check.executeQuery()) { if (rs.next()) return false; }
        } catch (SQLException e) { plugin.getLogger().warning("Vérification demande active impossible : " + e.getMessage()); return false; }
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO team_requests(team_id,creator_uuid,type,material,item_data,target,created_at) VALUES(?,?,?,?,?,?,?)")) {
            ps.setInt(1, teamId); ps.setString(2, creator.toString()); ps.setString(3, type); ps.setString(4, material); ps.setString(5, itemData); ps.setDouble(6, target); ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate(); return true;
        } catch (SQLException e) { plugin.getLogger().warning("Création demande équipe impossible : " + e.getMessage()); return false; }
    }

    public synchronized List<TeamRequest> active(int teamId) {
        List<TeamRequest> out = new ArrayList<>();
        if (connection == null) return out;
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM team_requests WHERE team_id=? AND active=1 ORDER BY id DESC")) {
            ps.setInt(1, teamId); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(new TeamRequest(rs.getInt("id"), rs.getInt("team_id"), rs.getString("type"), rs.getString("material"), rs.getString("item_data"), rs.getDouble("target"), rs.getDouble("collected"), rs.getInt("active") == 1)); }
        } catch (SQLException e) { plugin.getLogger().warning("Lecture demandes équipe impossible : " + e.getMessage()); }
        return out;
    }

    public synchronized TeamRequest get(int id, int teamId) {
        if (connection == null) return null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM team_requests WHERE id=? AND team_id=? AND active=1")) {
            ps.setInt(1,id); ps.setInt(2,teamId); try (ResultSet rs=ps.executeQuery()) { if(rs.next()) return new TeamRequest(rs.getInt("id"),rs.getInt("team_id"),rs.getString("type"),rs.getString("material"),rs.getString("item_data"),rs.getDouble("target"),rs.getDouble("collected"),true); }
        } catch (SQLException e) { plugin.getLogger().warning("Lecture demande équipe impossible : " + e.getMessage()); }
        return null;
    }

    public synchronized boolean addContribution(TeamRequest request, Player player, double amount) {
        if (connection == null || request == null || amount <= 0 || amount > request.remaining() + 0.000001) return false;
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement update = connection.prepareStatement("UPDATE team_requests SET collected=collected+?, active=CASE WHEN collected+?>=target THEN 0 ELSE 1 END WHERE id=? AND team_id=? AND active=1 AND collected+?<=target")) {
                update.setDouble(1,amount); update.setDouble(2,amount); update.setInt(3,request.id()); update.setInt(4,request.teamId()); update.setDouble(5,amount);
                if (update.executeUpdate() != 1) { connection.rollback(); return false; }
            }
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO team_request_contributions(request_id,team_id,player_uuid,player_name,type,amount) VALUES(?,?,?,?,?,?) ON CONFLICT(request_id,player_uuid,type) DO UPDATE SET amount=amount+excluded.amount, player_name=excluded.player_name")) {
                ps.setInt(1,request.id()); ps.setInt(2,request.teamId()); ps.setString(3,player.getUniqueId().toString()); ps.setString(4,player.getName()); ps.setString(5,request.type()); ps.setDouble(6,amount); ps.executeUpdate();
            }
            connection.commit(); return true;
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            plugin.getLogger().warning("Enregistrement contribution équipe impossible : " + e.getMessage()); return false;
        } finally { try { connection.setAutoCommit(true); } catch (SQLException ignored) {} }
    }

    public synchronized List<ContributionTotal> leaderboard(int teamId) {
        Map<String, ContributionTotal> totals = new LinkedHashMap<>();
        if (connection == null) return new ArrayList<>();
        String sql = "SELECT player_uuid, player_name, SUM(CASE WHEN type='MONEY' THEN amount ELSE 0 END) money, SUM(CASE WHEN type='ITEM' THEN amount ELSE 0 END) items FROM team_request_contributions WHERE team_id=? GROUP BY player_uuid,player_name ORDER BY money DESC, items DESC";
        try (PreparedStatement ps=connection.prepareStatement(sql)) { ps.setInt(1,teamId); try(ResultSet rs=ps.executeQuery()) { while(rs.next()) totals.put(rs.getString("player_uuid"),new ContributionTotal(rs.getString("player_name"),rs.getDouble("money"),(long)rs.getDouble("items"))); } }
        catch(SQLException e) { plugin.getLogger().warning("Classement contributions impossible : " + e.getMessage()); }
        return new ArrayList<>(totals.values());
    }

    public void prompt(Player player, String type, int requestId) { prompts.put(player.getUniqueId(), new Prompt(type, requestId, null)); }
    public void prompt(Player player, String type, int requestId, String itemData) { prompts.put(player.getUniqueId(), new Prompt(type, requestId, itemData)); }
    public static String serializeItem(ItemStack item) { try { ItemStack one=item.clone(); one.setAmount(1); return ItemSerialization.toBase64(one); } catch (IOException e) { return null; } }
    public static ItemStack deserializeItem(String data) { if(data==null || data.isBlank()) return null; try { return ItemSerialization.fromBase64(data); } catch (IOException | ClassNotFoundException e) { return null; } }
    public Prompt takePrompt(UUID uuid) { return prompts.remove(uuid); }
    public boolean hasPrompt(UUID uuid) { return prompts.containsKey(uuid); }
    public void cancelPrompt(UUID uuid) { prompts.remove(uuid); }

    public void close() { if (connection != null) try { connection.close(); } catch (SQLException ignored) {} }
}
