package fr.crewcmoi.teleport.utils;

import fr.crewcmoi.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Utilitaire pour l'envoi des messages de demande de téléportation, avec un
 * bouton [Teleporter] cliquable qui exécute /tpaccept.
 */
public final class TeleportMessages {

    private TeleportMessages() {
    }

    /**
     * Envoie au receveur le message de demande reçue (clé messages.yml donnée), suivi
     * d'un bouton [Teleporter] cliquable qui accepte la demande.
     */
    public static void sendRequestReceived(Main plugin, Player target, Player requester, String messageKey) {
        String rawMessage = plugin.getMessages().getString("prefix", "") +
                plugin.getMessages().getString(messageKey, "&e{player}&a souhaite se téléporter.")
                        .replace("{player}", requester.getName());

        Component textComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage);
        target.sendMessage(textComponent);

        String buttonRaw = plugin.getMessages().getString("tpa.accept-button", "&a&l[Teleporter]");
        Component button = LegacyComponentSerializer.legacyAmpersand().deserialize(buttonRaw)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/tpaccept"))
                .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour accepter", NamedTextColor.GREEN)));

        target.sendMessage(button);
    }
}
