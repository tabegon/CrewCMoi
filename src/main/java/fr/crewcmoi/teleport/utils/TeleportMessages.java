package fr.crewcmoi.teleport.utils;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public final class TeleportMessages {

    private TeleportMessages() {
    }

    public static void sendRequestReceived(Main plugin, Player target, Player requester, String messageKey) {
        String rawMessage = plugin.getMessages().getString("prefix") +
                plugin.getMessages().getString(messageKey)
                        .replace("{player}", requester.getName());

        Component textComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage);
        target.sendMessage(textComponent);

        String buttonRaw = plugin.getMessages().getString("tpa.accept-button");
        Component button = LegacyComponentSerializer.legacyAmpersand().deserialize(buttonRaw)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/tpaccept"))
                .hoverEvent(HoverEvent.showText(Component.text(Messages.get("tpa.accept-hover"), NamedTextColor.GREEN)));

        target.sendMessage(button);
    }
}
