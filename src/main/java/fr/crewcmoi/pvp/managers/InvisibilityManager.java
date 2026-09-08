package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.managers.PlayerTeamManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class InvisibilityManager {

    public static final String ANONYMOUS_NAME = "Anonyme";

    private final Main plugin;
    private final PlayerTeamManager playerTeamManager;

    public InvisibilityManager(Main plugin, PlayerTeamManager playerTeamManager) {
        this.plugin = plugin;
        this.playerTeamManager = playerTeamManager;
    }

    public void hideNameTag(Player player) {
        playerTeamManager.setNameTagHidden(player, true);
    }

    public void showNameTag(Player player) {
        playerTeamManager.setNameTagHidden(player, false);
    }

    public boolean isAnonymous(Player player) {
        return player != null && player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }
}
