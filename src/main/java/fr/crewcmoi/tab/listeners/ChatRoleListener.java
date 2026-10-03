package fr.crewcmoi.tab.listeners;

import fr.crewcmoi.tab.roles.RoleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Ajoute le préfixe du rôle du joueur devant son nom dans le chat.
 *
 * Le préfixe est récupéré depuis le même RoleManager que celui utilisé par
 * le TAB. Le joueur voit donc exactement son rôle (Fonda, Dev, Mod, VIP ou
 * Player) dans le chat comme dans la liste des joueurs.
 */
public class ChatRoleListener implements Listener {

    private final RoleManager roleManager;

    public ChatRoleListener(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String prefix = roleManager.getPrefix(roleManager.getRole(player));

        // AsyncPlayerChatEvent utilise String.format : les éventuels '%' du
        // préfixe doivent donc être échappés pour ne pas casser le format.
        prefix = prefix.replace("%", "%%");

        event.setFormat(prefix + "%1$s §8» §f%2$s");
    }
}
