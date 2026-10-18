package fr.crewcmoi.economie.listeners;

import fr.crewcmoi.Main;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/** Bloque /wardrobe avant la fin de la quête 2. */
public final class QuestCommandListener implements Listener {
    private final Main plugin;

    public QuestCommandListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || !message.startsWith("/")) return;

        String command = message.substring(1).trim();
        if (command.isEmpty()) return;

        String base = command.split("\\s+", 2)[0].toLowerCase();
        if (!base.equals("wardrobe") && !base.equals("wardrobe:wardrobe")) return;

        // /wardrobe reste volontairement interdit pour tout le monde.
        // L'accès aux cosmétiques se fait uniquement en entrant dans la salle
        // de wardrobe de CosmeticsCore, après validation de la quête 2.
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "Accès uniquement depuis la salle des cosmétiques.");
    }
}
