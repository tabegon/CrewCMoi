package fr.crewcmoi.pvp.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.BountyManager;
import fr.crewcmoi.pvp.managers.CombatManager;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.pvp.managers.InvisibilityManager;
import fr.crewcmoi.pvp.managers.TeamManager;
import fr.crewcmoi.tab.managers.PlayerTeamManager;
import fr.crewcmoi.other.utils.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import fr.crewcmoi.other.utils.MoneyFormat;
import java.util.UUID;

public class BountyListener implements Listener {

    private final Main plugin;
    private final BountyManager bountyManager;
    private final CombatManager combatManager;
    private final TeamManager teamManager;
    private final EconomyManager economyManager;
    private final InvisibilityManager invisibilityManager;
    private final PlayerTeamManager playerTeamManager;

    public BountyListener(Main plugin, BountyManager bountyManager, CombatManager combatManager,
                           TeamManager teamManager, EconomyManager economyManager,
                           InvisibilityManager invisibilityManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
        this.combatManager = combatManager;
        this.teamManager = teamManager;
        this.economyManager = economyManager;
        this.invisibilityManager = invisibilityManager;
        this.playerTeamManager = plugin.getPlayerTeamManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeathAnonymizeInvisibleKiller(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        if (!invisibilityManager.isAnonymous(killer)) {
            return;
        }

        String deathMessage = event.getDeathMessage();
        if (deathMessage != null) {
            String prefix = playerTeamManager.getPrefix(killer.getUniqueId());
            String suffix = playerTeamManager.getSuffix(killer.getUniqueId());
            String fullDisplayName = prefix + killer.getName() + suffix;

            
            
            String anonymized = deathMessage.replace(fullDisplayName, InvisibilityManager.ANONYMOUS_NAME);
            if (anonymized.equals(deathMessage)) {
                anonymized = deathMessage.replace(killer.getName(), InvisibilityManager.ANONYMOUS_NAME);
            }
            event.setDeathMessage(anonymized);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            combatManager.stopCombatForBoth(victim);
            return;
        }

        

        UUID aggressor = combatManager.getAggressor(victim);
        boolean killerInitiated = aggressor != null && aggressor.equals(killer.getUniqueId());

        combatManager.stopCombatForBoth(victim);

        String currency = plugin.getConfig().getString("economy.currency-symbol");

        

        

        UUID killerUuid = killer.getUniqueId();
        BountyManager.BountyClaimResult claim = bountyManager.claimBounty(killerUuid, victim.getUniqueId(),
                contributorUuid -> contributorUuid.equals(killerUuid) || teamManager.isSameTeam(contributorUuid, killerUuid));
        if (claim.amount() > 0) {
            Messages.send(killer, "bounty.claimed-kill", java.util.Map.of("amount", MoneyFormat.format(claim.amount()) + currency, "player", victim.getName()));
        }
        if (claim.legitimate()) {
            return;
        }

        

        if (!killerInitiated) {
            
            return;
        }

        

        
        
        fr.crewcmoi.pvp.managers.DuelSession duelSession = plugin.getDuelManager().getSession(killer.getUniqueId());
        if (duelSession != null && duelSession.involves(victim.getUniqueId())) {
            return;
        }

        if (teamManager.hasTeam(victim.getUniqueId())) {
            
            return;
        }

        int countToday = bountyManager.getServerBountyCountToday(killer.getUniqueId());
        int maxPerDay = plugin.getConfig().getInt("bounty.server-bounty-max-per-day", 3);
        if (countToday >= maxPerDay) {
            return;
        }

        double bountyAmount = plugin.getConfig().getDouble("bounty.server-bounty-amount", 50.0);
        double malusAmount = plugin.getConfig().getDouble("bounty.server-malus-amount", 50.0);

        economyManager.withdraw(killer.getUniqueId(), malusAmount);
        bountyManager.addServerBounty(killer.getUniqueId(), killer.getName(), bountyAmount);
        bountyManager.incrementServerBountyCount(killer.getUniqueId());

        

        

        Messages.send(killer, "bounty.server-malus", java.util.Map.of("player", victim.getName(), "malus", MoneyFormat.format(malusAmount) + currency, "bounty", MoneyFormat.format(bountyAmount) + currency));
    }

    private void sendMessage(Player player, String message) {
        String prefix = plugin.getMessages().getString("prefix");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
