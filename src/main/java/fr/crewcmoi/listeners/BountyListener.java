package fr.crewcmoi.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.managers.BountyManager;
import fr.crewcmoi.managers.CombatManager;
import fr.crewcmoi.managers.EconomyManager;
import fr.crewcmoi.managers.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import fr.crewcmoi.utils.MoneyFormat;
import java.util.UUID;

/**
 * Gère les conséquences économiques d'une mort en JcJ :
 *  - si la victime avait une prime active, le tueur la récupère (hors contributions qu'il a lui-même placées).
 *  - sinon, si le tueur a initié le combat (premier coup) et que la victime n'a pas de prime,
 *    le serveur lui attribue automatiquement une prime + un malus, sauf si :
 *      - la victime fait partie d'une équipe (les membres d'équipe ne déclenchent pas ce malus),
 *      - le tueur s'est déjà vu attribuer cette prime serveur 3 fois aujourd'hui,
 *      - le tueur se défendait (c'est la victime qui avait initié le combat).
 */
public class BountyListener implements Listener {

    private final Main plugin;
    private final BountyManager bountyManager;
    private final CombatManager combatManager;
    private final TeamManager teamManager;
    private final EconomyManager economyManager;

    public BountyListener(Main plugin, BountyManager bountyManager, CombatManager combatManager,
                           TeamManager teamManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
        this.combatManager = combatManager;
        this.teamManager = teamManager;
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            combatManager.stopCombatForBoth(victim);
            return;
        }

        // Important : on lit qui a initié ce combat AVANT de nettoyer le combat log,
        // car stopCombatForBoth() efface justement cette information (engagementAggressor).
        // L'inverser faisait que getAggressor(victim) renvoyait toujours null, et donc que
        // la prime + malus serveur n'était jamais attribuée.
        UUID aggressor = combatManager.getAggressor(victim);
        boolean killerInitiated = aggressor != null && aggressor.equals(killer.getUniqueId());

        // Le combat log des deux joueurs impliqués s'arrête immédiatement à la mort.
        combatManager.stopCombatForBoth(victim);

        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

        double claimed = bountyManager.claimBounty(killer.getUniqueId(), victim.getUniqueId());
        if (claimed > 0) {
            sendMessage(killer, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ʀᴇᴄᴜᴘᴇʀᴇ ᴜɴᴇ ᴘʀɪᴍᴇ ᴅᴇ &e" + MoneyFormat.format(claimed) + currency
                    + "&a ᴇɴ ᴛᴜᴀɴᴛ &e" + victim.getName() + "&a !");
            return;
        }

        // Pas de prime réclamable : on vérifie si une prime + malus serveur doit être attribuée.
        if (!killerInitiated) {
            // Le tueur se défendait : pas de malus, conformément à la règle.
            return;
        }

        if (teamManager.hasTeam(victim.getUniqueId())) {
            // La victime fait partie d'une équipe : pas de malus attribué à l'agresseur.
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

        // Les effets de malus (réduction de dégâts contre les joueurs, puis retrait permanent
        // de coeurs à partir d'un certain montant) sont recalculés automatiquement en fonction
        // du nouveau total de prime serveur du tueur (voir BountyManager#addServerBounty ->
        // refreshBountyDisplay -> MalusEffectManager#updateBountyEffect), pas besoin de les
        // déclencher ici manuellement.

        sendMessage(killer, "&cᴠᴏᴜꜱ ᴀᴠᴇᴢ ᴛᴜᴇ &e" + victim.getName() + "&c ꜱᴀɴꜱ ǫᴜ'ɪʟ ɴ'ᴀɪᴛ ᴅᴇ ᴘʀɪᴍᴇ : "
                + "ʟᴇ ꜱᴇʀᴠᴇᴜʀ ᴠᴏᴜꜱ ɪɴꜰʟɪɢᴇ ᴜɴ ᴍᴀʟᴜꜱ ᴅᴇ &e" + MoneyFormat.format(malusAmount) + currency
                + "&c ᴇᴛ ᴘʟᴀᴄᴇ ᴜɴᴇ ᴘʀɪᴍᴇ ᴅᴇ &e" + MoneyFormat.format(bountyAmount) + currency + "&c ꜱᴜʀ ᴠᴏᴜꜱ !");
    }

    private void sendMessage(Player player, String message) {
        String prefix = plugin.getMessages().getString("prefix", "");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
