package fr.crewcmoi.pvp.placeholder;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.BountyManager;
import fr.crewcmoi.other.utils.MoneyFormat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class BountyPlaceholderExpansion extends PlaceholderExpansion {

    private final Main plugin;
    private final BountyManager bountyManager;

    public BountyPlaceholderExpansion(Main plugin, BountyManager bountyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "crewcmoi";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MonsieurTh30";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    
    
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }

        double total = bountyManager.getCachedBountyTotal(player.getUniqueId());

        if (params.equalsIgnoreCase("bounty")) {
            return total > 0 ? MoneyFormat.format(total) : "";
        }

        if (params.equalsIgnoreCase("bounty_suffix")) {
            return total > 0 ? ("§6 " + MoneyFormat.format(total) + " §f") : "";
        }

        if (params.equalsIgnoreCase("has_bounty")) {
            return String.valueOf(total > 0);
        }

        return null;
    }
}
