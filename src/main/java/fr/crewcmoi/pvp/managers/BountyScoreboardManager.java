package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.managers.PlayerTeamManager;
import fr.crewcmoi.other.utils.MoneyFormat;
import org.bukkit.ChatColor;

import java.util.UUID;

public class BountyScoreboardManager {

    private final Main plugin;
    private final PlayerTeamManager playerTeamManager;

    public BountyScoreboardManager(Main plugin, PlayerTeamManager playerTeamManager) {
        this.plugin = plugin;
        this.playerTeamManager = playerTeamManager;
    }

    public void update(UUID targetUuid, String targetName, double total) {
        if (total <= 0) {
            remove(targetUuid);
            return;
        }

        String suffix = ChatColor.GOLD + " [" + MoneyFormat.format(total) + " " + ChatColor.WHITE + "\uE517" + ChatColor.GOLD + "]";
        playerTeamManager.setSuffix(targetUuid, targetName, suffix);
    }

    public void remove(UUID targetUuid) {
        playerTeamManager.clearSuffix(targetUuid);
    }
}
