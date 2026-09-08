package fr.crewcmoi.pvp.utils;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public final class DuelMessages {

    private DuelMessages() {
    }

    public static void sendRequestReceived(Main plugin, Player target, Player requester,
                                            boolean keepInventory, String betText, boolean dropHead, String kitName, double kitPrice) {
        String rawMessage = plugin.getMessages().getString("prefix") +
                plugin.getMessages().getString("duel.received")
                        .replace("{player}", requester.getName());
        target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage));

        String rulesRaw = plugin.getMessages().getString("prefix") +
                plugin.getMessages().getString("duel.received-rules")
                        .replace("{keepinventory}", keepInventory ? "&aoui" : "&cnon")
                        .replace("{bet}", "&e" + betText)
                        .replace("{drophead}", dropHead ? "&aoui" : "&cnon")
                        .replace("{kit}", kitName == null ? "&7aucun" : "&e" + kitName)
                        .replace("{kitprice}", kitName == null ? "0" : MoneyFormat.format(kitPrice));
        target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(rulesRaw));

        String acceptRaw = plugin.getMessages().getString("duel.accept-button");
        Component acceptButton = LegacyComponentSerializer.legacyAmpersand().deserialize(acceptRaw)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/duelaccept"))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        plugin.getMessages().getString("duel.accept-hover"))));

        String denyRaw = plugin.getMessages().getString("duel.deny-button");
        Component denyButton = LegacyComponentSerializer.legacyAmpersand().deserialize(denyRaw)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/duelaccept deny"))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        plugin.getMessages().getString("duel.deny-hover"))));

        target.sendMessage(acceptButton.append(Component.text("   ")).append(denyButton));
    }
}
