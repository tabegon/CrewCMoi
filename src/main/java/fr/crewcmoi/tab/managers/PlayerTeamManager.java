package fr.crewcmoi.tab.managers;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTeamManager {

    private static final String TEAM_PREFIX = "ccp_";

    private final Main plugin;

    private final Map<Scoreboard, Map<UUID, Team>> teamsByScoreboard = new IdentityHashMap<>();

    private final Map<UUID, String> prefixes = new ConcurrentHashMap<>();
    private final Map<UUID, String> suffixes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> hiddenNameTags = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> sortWeights = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public PlayerTeamManager(Main plugin) {
        this.plugin = plugin;
    }

    private static final String[] LEGACY_TEAM_PREFIXES = {"cc_role_", "cc_invisible", "ccbounty_", "r0_", "r1_", "r2_", "r3_", "r4_", "r5_"};

    public void start() {
        for (Scoreboard scoreboard : onlineScoreboards()) {
            for (Team team : scoreboard.getTeams().toArray(new Team[0])) {
                String name = team.getName();
                if (name.startsWith(TEAM_PREFIX) || startsWithAny(name, LEGACY_TEAM_PREFIXES)) {
                    team.unregister();
                }
            }
        }
    }

    private boolean startsWithAny(String value, String[] prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public void stop() {
        for (Map<UUID, Team> teams : teamsByScoreboard.values()) {
            for (Team team : teams.values()) {
                if (team.getScoreboard() != null) {
                    team.unregister();
                }
            }
        }
        teamsByScoreboard.clear();
    }

    public void setPrefix(Player player, String prefix, int sortWeight) {
        UUID uuid = player.getUniqueId();
        names.put(uuid, player.getName());
        prefixes.put(uuid, prefix == null ? "" : prefix);
        sortWeights.put(uuid, sortWeight);
        applyToAllScoreboards(uuid);
    }

    public void setSuffix(UUID uuid, String targetName, String suffix) {
        names.put(uuid, targetName);
        if (suffix == null) {
            suffixes.remove(uuid);
        } else {
            suffixes.put(uuid, suffix);
        }
        applyToAllScoreboards(uuid);
    }

    public String getPrefix(UUID uuid) {
        return prefixes.getOrDefault(uuid, "");
    }

    public String getSuffix(UUID uuid) {
        return suffixes.getOrDefault(uuid, "");
    }

    public void setTabFormattingHidden(UUID uuid, boolean hidden) {
        String entryName = names.get(uuid);
        if (entryName == null) {
            return;
        }
        for (Scoreboard scoreboard : onlineScoreboards()) {
            setTabFormattingHidden(scoreboard, uuid, hidden);
        }
    }

    private void setTabFormattingHidden(Scoreboard scoreboard, UUID uuid, boolean hidden) {
        String entryName = names.get(uuid);
        if (scoreboard == null || entryName == null) {
            return;
        }

        int weight = sortWeights.getOrDefault(uuid, 99);
        String expectedName = teamName(uuid, weight);
        Map<UUID, Team> teams = teamsByScoreboard.computeIfAbsent(scoreboard, s -> new HashMap<>());
        Team team = teams.get(uuid);

        if (team == null || team.getScoreboard() == null || !team.getName().equals(expectedName)) {
            applyTo(scoreboard, uuid);
            team = teams.get(uuid);
        }

        if (team == null) {
            return;
        }

        if (!team.hasEntry(entryName)) {
            team.addEntry(entryName);
        }

        team.setPrefix(hidden ? "" : cut(prefixes.getOrDefault(uuid, ""), 64));
        team.setSuffix(hidden ? "" : cut(suffixes.getOrDefault(uuid, ""), 64));
    }

    public void clearSuffix(UUID uuid) {
        suffixes.remove(uuid);
        if (names.containsKey(uuid)) {
            applyToAllScoreboards(uuid);
        }
    }

    public void setNameTagHidden(Player player, boolean hidden) {
        UUID uuid = player.getUniqueId();
        names.put(uuid, player.getName());
        hiddenNameTags.put(uuid, hidden);
        applyToAllScoreboards(uuid);
    }

    public void refreshOnJoin(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        for (UUID uuid : names.keySet()) {
            applyTo(scoreboard, uuid);
        }
    }

    public void clear(Player player) {
        UUID uuid = player.getUniqueId();
        prefixes.remove(uuid);
        suffixes.remove(uuid);
        hiddenNameTags.remove(uuid);
        sortWeights.remove(uuid);
        names.remove(uuid);
        for (Map<UUID, Team> teams : teamsByScoreboard.values()) {
            Team team = teams.remove(uuid);
            if (team != null && team.getScoreboard() != null) {
                team.unregister();
            }
        }
    }

    private void applyToAllScoreboards(UUID uuid) {
        for (Scoreboard scoreboard : onlineScoreboards()) {
            applyTo(scoreboard, uuid);
        }
    }

    private void applyTo(Scoreboard scoreboard, UUID uuid) {
        String entryName = names.get(uuid);
        if (scoreboard == null || entryName == null) {
            return;
        }

        int weight = sortWeights.getOrDefault(uuid, 99);
        String expectedName = teamName(uuid, weight);

        Map<UUID, Team> teams = teamsByScoreboard.computeIfAbsent(scoreboard, s -> new HashMap<>());
        Team team = teams.get(uuid);

        if (team != null && (team.getScoreboard() == null || !team.getName().equals(expectedName))) {

            if (team.getScoreboard() != null) {
                team.unregister();
            }
            team = null;
        }

        if (team == null) {
            team = scoreboard.getTeam(expectedName);
            if (team == null) {
                team = scoreboard.registerNewTeam(expectedName);
            }
            teams.put(uuid, team);
        }

        if (!team.hasEntry(entryName)) {
            team.addEntry(entryName);
        }

        
        team.setPrefix(cut(prefixes.getOrDefault(uuid, ""), 64));
        team.setSuffix(cut(suffixes.getOrDefault(uuid, ""), 64));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY,
                hiddenNameTags.getOrDefault(uuid, false) ? Team.OptionStatus.NEVER : Team.OptionStatus.ALWAYS);
    }

    private Set<Scoreboard> onlineScoreboards() {
        Set<Scoreboard> scoreboards = Collections.newSetFromMap(new IdentityHashMap<>());
        scoreboards.add(Bukkit.getScoreboardManager().getMainScoreboard());
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            scoreboards.add(online.getScoreboard());
        }
        return scoreboards;
    }

    private String teamName(UUID uuid, int weight) {
        String compact = uuid.toString().replace("-", "");
        String weightPart = String.format("%02d", Math.max(0, Math.min(99, weight)));
        String prefix = TEAM_PREFIX + weightPart + "_";
        int available = 16 - prefix.length();
        return prefix + compact.substring(0, Math.min(Math.max(available, 0), compact.length()));
    }

    private String cut(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
