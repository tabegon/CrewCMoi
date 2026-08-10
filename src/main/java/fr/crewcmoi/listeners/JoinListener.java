package fr.crewcmoi.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.managers.BountyManager;
import fr.crewcmoi.managers.EconomyManager;
import fr.crewcmoi.managers.MalusEffectManager;
import fr.crewcmoi.managers.TeamManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinListener implements Listener {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final TeamManager teamManager;
    private final BountyManager bountyManager;
    private final MalusEffectManager malusEffectManager;

    public JoinListener(Main plugin, EconomyManager economyManager, TeamManager teamManager,
                         BountyManager bountyManager, MalusEffectManager malusEffectManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.teamManager = teamManager;
        this.bountyManager = bountyManager;
        this.malusEffectManager = malusEffectManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        economyManager.loadPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        teamManager.loadPlayer(event.getPlayer().getUniqueId());
        // Recalcule au passage les effets de malus (réduction de dégâts + coeurs retirés)
        // liés à sa prime serveur, au cas où elle aurait changé pendant qu'il était déconnecté.
        bountyManager.refreshBountyDisplayOnJoin(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        economyManager.unloadPlayer(event.getPlayer().getUniqueId());
        teamManager.unloadPlayer(event.getPlayer().getUniqueId());
        malusEffectManager.clearVolatileState(event.getPlayer());
    }
}

