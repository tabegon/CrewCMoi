package fr.crewcmoi.claims.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.gui.ClaimAuctionGuiManager;
import fr.crewcmoi.claims.gui.ClaimSettingsGuiManager;
import fr.crewcmoi.claims.gui.ClaimShopGuiManager;
import fr.crewcmoi.claims.managers.ClaimManager;
import fr.crewcmoi.claims.managers.ClaimVisualizer;
import fr.crewcmoi.other.utils.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClaimCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ClaimManager claimManager;
    private final ClaimSettingsGuiManager claimSettingsGuiManager;
    private final ClaimShopGuiManager claimShopGuiManager;
    private final ClaimVisualizer claimVisualizer;
    private final ClaimAuctionGuiManager claimAuctionGuiManager;

    public ClaimCommand(Main plugin, ClaimManager claimManager, ClaimSettingsGuiManager claimSettingsGuiManager,
                         ClaimShopGuiManager claimShopGuiManager, ClaimVisualizer claimVisualizer,
                         ClaimAuctionGuiManager claimAuctionGuiManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.claimSettingsGuiManager = claimSettingsGuiManager;
        this.claimShopGuiManager = claimShopGuiManager;
        this.claimVisualizer = claimVisualizer;
        this.claimAuctionGuiManager = claimAuctionGuiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, "claims.command.player-only");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            handleClaim(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "unclaim":
                handleUnclaim(player);
                break;
            case "trust":
                if (args.length < 2) {
                    Messages.send(player, "claims.command.trust-usage");
                    return true;
                }
                handleTrust(player, args[1], true);
                break;
            case "untrust":
                if (args.length < 2) {
                    Messages.send(player, "claims.command.untrust-usage");
                    return true;
                }
                handleTrust(player, args[1], false);
                break;
            case "info":
                handleInfo(player);
                break;
            case "settings":
                handleSettings(player);
                break;
            case "see":
                handleSee(player);
                break;
            case "shop":
                claimShopGuiManager.open(player);
                break;
            case "sell":
                handleSell(player, args);
                break;
            case "buy":
                handleBuy(player);
                break;
            case "ah":
                claimAuctionGuiManager.open(player, 0);
                break;
            default:
                Messages.send(player, "claims.command.usage");
                break;
        }

        return true;
    }

    private void handleClaim(Player player) {
        ClaimManager.ClaimResult result = claimManager.claim(player);
        switch (result) {
            case SUCCESS:
                sendMessage(player, "claim.success");
                break;
            case ALREADY_CLAIMED:
                sendMessage(player, "claim.already-claimed");
                break;
            case LIMIT_REACHED:
                String def = "&cVous avez atteint votre nombre maximum de claims (&e{max}&c). Utilisez /claim shop pour en acheter davantage.";
                String message = plugin.getMessages().getString("claim.limit-reached", def)
                        .replace("{max}", String.valueOf(claimManager.getMaxClaims(player)));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix") + message));
                break;
        }
    }

    private void handleUnclaim(Player player) {
        ClaimManager.UnclaimResult result = claimManager.unclaim(player);
        switch (result) {
            case SUCCESS:
                sendMessage(player, "claim.unclaim-success");
                break;
            case NOT_CLAIMED:
                sendMessage(player, "claim.not-claimed");
                break;
            case NOT_OWNER:
                sendMessage(player, "claim.not-owner");
                break;
            case ERROR:
                sendMessage(player, "claim.unclaim-error");
                break;
        }
    }

    private void handleTrust(Player player, String targetName, boolean trust) {
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sendMessage(player, "claim.player-not-found");
            return;
        }

        ClaimManager.TrustResult result = trust
                ? claimManager.trust(player, target.getUniqueId(), targetName)
                : claimManager.untrust(player, target.getUniqueId());

        switch (result) {
            case SUCCESS:
                String key = trust ? "claim.trust-success" : "claim.untrust-success";
                String def = trust ? "&a{player} peut maintenant construire sur ce claim."
                        : "&a{player} ne peut plus construire sur ce claim.";
                String message = plugin.getMessages().getString(key, def).replace("{player}", targetName);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix") + message));
                break;
            case NOT_CLAIMED:
                sendMessage(player, "claim.not-claimed");
                break;
            case NOT_OWNER:
                sendMessage(player, "claim.not-owner");
                break;
        }
    }

    private void handleInfo(Player player) {
        var chunk = player.getLocation().getChunk();
        ClaimData claim = claimManager.getClaimResynced(chunk);
        if (claim == null) {
            sendMessage(player, "claim.not-claimed");
            return;
        }
        Messages.send(player, "claims.command.info-owner", java.util.Map.of("owner", claim.getOwnerName()), false);
        if (!claim.getTrusted().isEmpty()) {
            Messages.send(player, "claims.command.info-trusted", java.util.Map.of("count", claim.getTrusted().size()), false);
        }
        if (claim.isForSale()) {
            Messages.send(player, "claims.command.info-for-sale", java.util.Map.of("price", MoneyFormat.format(claim.getSalePrice())), false);
        }
    }

    private void handleSettings(Player player) {
        var chunk = player.getLocation().getChunk();
        ClaimData claim = claimManager.getClaimResynced(chunk);
        if (claim == null) {
            sendMessage(player, "claim.not-claimed");
            return;
        }
        if (!claim.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("crew.claim.admin")) {
            sendMessage(player, "claim.not-owner");
            return;
        }
        claimSettingsGuiManager.open(player, chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    private void handleSee(Player player) {
        boolean nowActive = claimVisualizer.toggle(player);
        if (nowActive) {
            sendMessage(player, "claim.see-enabled");
        } else {
            sendMessage(player, "claim.see-disabled");
        }
    }

    private void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            Messages.send(player, "claims.command.sell-usage");
            return;
        }

        if (args[1].equalsIgnoreCase("cancel")) {
            ClaimManager.SellResult result = claimManager.cancelSale(player);
            switch (result) {
                case SUCCESS:
                    sendMessage(player, "claim.sale-cancelled");
                    break;
                case NOT_CLAIMED:
                    sendMessage(player, "claim.not-claimed");
                    break;
                case NOT_OWNER:
                    sendMessage(player, "claim.not-owner");
                    break;
                case NOT_FOR_SALE:
                    sendMessage(player, "claim.not-for-sale");
                    break;
                default:
                    break;
            }
            return;
        }

        double price;
        try {
            price = MoneyFormat.parse(args[1]);
        } catch (NumberFormatException e) {
            Messages.send(player, "claims.command.sell-usagex");
            return;
        }

        ClaimManager.SellResult result = claimManager.sellClaim(player, price);
        switch (result) {
            case SUCCESS:
                String def = "&aCe claim est maintenant en vente pour &e{price}&a. Utilisez /claim sell cancel pour annuler.";
                String message = plugin.getMessages().getString("claim.sale-started", def)
                        .replace("{price}", MoneyFormat.format(price));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix") + message));
                break;
            case INVALID_PRICE:
                sendMessage(player, "claim.invalid-price");
                break;
            case NOT_CLAIMED:
                sendMessage(player, "claim.not-claimed");
                break;
            case NOT_OWNER:
                sendMessage(player, "claim.not-owner");
                break;
            default:
                break;
        }
    }

    private void handleBuy(Player player) {
        ClaimManager.BuyResult result = claimManager.buyClaim(player);
        switch (result) {
            case SUCCESS:
                sendMessage(player, "claim.buy-success");
                break;
            case NOT_CLAIMED:
                sendMessage(player, "claim.not-claimed");
                break;
            case NOT_FOR_SALE:
                sendMessage(player, "claim.not-for-sale");
                break;
            case OWN_CLAIM:
                sendMessage(player, "claim.own-claim");
                break;
            case NOT_ENOUGH_MONEY:
                sendMessage(player, "claim.not-enough-money");
                break;
            case LIMIT_REACHED:
                String message = plugin.getMessages().getString("claim.limit-reached-buy")
                        .replace("{max}", String.valueOf(claimManager.getMaxClaims(player)));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix") + message));
                break;
        }
    }

    private void sendMessage(Player player, String key) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix") + plugin.getMessages().getString(key)));
        maybeHintNearbyOwnClaim(player, key);
    }

    private void maybeHintNearbyOwnClaim(Player player, String key) {
        if (!"claim.not-claimed".equals(key)) {
            return;
        }
        List<ClaimData> nearby = claimManager.getNearbyClaims(player.getLocation(), 1);
        boolean ownsAdjacent = nearby.stream().anyMatch(c -> c.getOwnerUuid().equals(player.getUniqueId()));
        if (ownsAdjacent) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("claim.nearby-own-claim-hint")));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("unclaim", "trust", "untrust", "info", "settings", "see", "shop", "sell", "buy", "ah");
            String partial = args[0].toLowerCase();
            return options.stream().filter(o -> o.startsWith(partial)).collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust"))) {
            String partial = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            String partial = args[1].toLowerCase();
            return "cancel".startsWith(partial) ? Arrays.asList("cancel") : new ArrayList<>();
        }
        return new ArrayList<>();
    }
}
