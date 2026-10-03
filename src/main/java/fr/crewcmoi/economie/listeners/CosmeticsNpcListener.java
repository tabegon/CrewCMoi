package fr.crewcmoi.economie.listeners;

import fr.crewcmoi.Main;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Empêche toute utilisation du NPC cosmétique avant la fin de la quête 2. */
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
        }
    }
}
