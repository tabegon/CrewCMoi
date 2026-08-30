package fr.crewcmoi.pvp.utils;

import fr.crewcmoi.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Utilitaire pour l'envoi du message de demande de duel reçue, avec les boutons
 * [Accepter] / [Refuser] cliquables (exécutent /duelaccept et /duelaccept deny).
 */
public final class DuelMessages {

    private DuelMessages() {
    }

    public static void sendRequestReceived(Main plugin, Player target, Player requester,
                                            boolean keepInventory, String betText) {
        String rawMessage = plugin.getMessages().getString("prefix", "") +
                plugin.getMessages().getString("duel.received", "&e{player}&a vous provoque en duel !")
                        .replace("{player}", requester.getName());
        target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage));

        String rulesRaw = plugin.getMessages().getString("prefix", "") +
                plugin.getMessages().getString("duel.received-rules",
                                "&7Keepinventory : {keepinventory} &7| Mise : {bet}")
                        .replace("{keepinventory}", keepInventory ? "&aoui" : "&cnon")
                        .replace("{bet}", "&e" + betText);
        target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(rulesRaw));

        String acceptRaw = plugin.getMessages().getString("duel.accept-button", "&a&l[Accepter]");
        Component acceptButton = LegacyComponentSerializer.legacyAmpersand().deserialize(acceptRaw)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/duelaccept"))
                .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour accepter", NamedTextColor.GREEN)));

        String denyRaw = plugin.getMessages().getString("duel.deny-button", "&c&l[Refuser]");
        Component denyButton = LegacyComponentSerializer.legacyAmpersand().deserialize(denyRaw)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/duelaccept deny"))
                .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour refuser", NamedTextColor.RED)));

        target.sendMessage(acceptButton.append(Component.text("   ")).append(denyButton));
    }
}
