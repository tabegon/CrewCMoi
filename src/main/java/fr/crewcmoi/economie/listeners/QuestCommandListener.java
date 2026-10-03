package fr.crewcmoi.economie.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/** Bloque totalement l'accès joueur à la commande /wardrobe. */
public final class QuestCommandListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || !message.startsWith("/")) return;

        String command = message.substring(1).trim();
        if (command.isEmpty()) return;

        String base = command.split("\\s+", 2)[0].toLowerCase();
        if (base.equals("wardrobe") || base.equals("wardrobe:wardrobe")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "La commande /wardrobe est désactivée.");
        }
    }
}
