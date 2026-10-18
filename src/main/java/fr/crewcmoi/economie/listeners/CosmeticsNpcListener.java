package fr.crewcmoi.economie.listeners;

import fr.crewcmoi.Main;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Gère l'accès au lieu de changement de cosmétiques après la quête 2. */
public final class CosmeticsNpcListener implements Listener {
    private final Main plugin;

    public CosmeticsNpcListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        if (event.getNPC().getId() != plugin.getConfig().getInt("npcs.cosmetics", 0)) return;

        Player player = event.getClicker();
        if (!plugin.getQuestManager().isQuest2Completed(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Tu dois terminer la deuxième quête du Quest Master avant d'utiliser les cosmétiques.");
            return;
        }

        // Le NPC ne lance volontairement aucune commande.
        // Après la quête 2, la permission CosmeticsCore d'entrée dans la salle
        // est accordée et CosmeticsCore gère lui-même l'accès à la zone.
        player.sendMessage(ChatColor.GREEN + "La salle des cosmétiques est maintenant accessible.");
    }
}
