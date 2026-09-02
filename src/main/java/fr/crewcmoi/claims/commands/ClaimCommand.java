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

/**
 * Commande /claim : gère le système de claims de chunks.
 * - /claim : réclame le chunk sur lequel se trouve le joueur.
 * - /claim unclaim : retire le claim du chunk actuel (si le joueur en est le propriétaire).
 * - /claim trust <joueur> : autorise un joueur à construire/détruire sur le claim actuel.
 * - /claim untrust <joueur> : retire cette autorisation.
 * - /claim info : affiche des informations sur le chunk actuel.
 * - /claim settings (ou /claims settings) : ouvre la GUI de configuration des règles du claim.
 * - /claim see : affiche par particules les claims aux alentours (vert = à vous, jaune =
 *   confiance, rouge = autres joueurs) pendant quelques secondes.
 * - /claim shop : ouvre la boutique pour acheter des claims supplémentaires.
 * - /claim sell <prix> : met en vente le claim actuel pour le prix donné (/claim sell cancel
 *   pour annuler la mise en vente).
 * - /claim buy : achète le claim actuel, s'il est mis en vente par son propriétaire.
 * - /claim ah : ouvre l'hôtel des ventes listant tous les claims à vendre sur le serveur,
 *   achetables directement depuis la GUI sans avoir besoin de s'y rendre.
 */
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
            Messages.send(sender, "server.claim-8660550");
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
                    Messages.send(player, "server.claim-e901053");
                    return true;
                }
                handleTrust(player, args[1], true);
                break;
            case "untrust":
                if (args.length < 2) {
                    Messages.send(player, "server.claim-5981061");
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
                Messages.send(player, "server.claim-24c7631");
                break;
        }

        return true;
    }

    private void handleClaim(Player player) {
        ClaimManager.ClaimResult result = claimManager.claim(player);
        switch (result) {
            case SUCCESS:
                sendMessage(player, "claim.success", "&aVous avez claim ce chunk. Personne d'autre ne peut y construire ou détruire.");
                break;
            case ALREADY_CLAIMED:
                sendMessage(player, "claim.already-claimed", "&cCe chunk est déjà claim.");
                break;
            case LIMIT_REACHED:
                String def = "&cVous avez atteint votre nombre maximum de claims (&e{max}&c). Utilisez /claim shop pour en acheter davantage.";
                String message = plugin.getMessages().getString("claim.limit-reached", def)
                        .replace("{max}", String.valueOf(claimManager.getMaxClaims(player)));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix", "") + message));
                break;
        }
    }

    private void handleUnclaim(Player player) {
        ClaimManager.UnclaimResult result = claimManager.unclaim(player);
        switch (result) {
            case SUCCESS:
                sendMessage(player, "claim.unclaim-success", "&aLe claim de ce chunk a été supprimé.");
                break;
            case NOT_CLAIMED:
                sendMessage(player, "claim.not-claimed", "&cCe chunk n'est pas claim.");
                break;
            case NOT_OWNER:
                sendMessage(player, "claim.not-owner", "&cVous n'êtes pas le propriétaire de ce claim.");
                break;
            case ERROR:
                sendMessage(player, "claim.unclaim-error", "&cUne erreur est survenue, le claim n'a pas été supprimé. Réessayez.");
                break;
        }
    }

    private void handleTrust(Player player, String targetName, boolean trust) {
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sendMessage(player, "claim.player-not-found", "&cJoueur introuvable.");
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
                        plugin.getMessages().getString("prefix", "") + message));
                break;
            case NOT_CLAIMED:
                sendMessage(player, "claim.not-claimed", "&cCe chunk n'est pas claim.");
                break;
            case NOT_OWNER:
                sendMessage(player, "claim.not-owner", "&cVous n'êtes pas le propriétaire de ce claim.");
                break;
        }
    }

    private void handleInfo(Player player) {
        var chunk = player.getLocation().getChunk();
        ClaimData claim = claimManager.getClaimResynced(chunk);
        if (claim == null) {
            sendMessage(player, "claim.not-claimed", "&cCe chunk n'est pas claim.");
            return;
        }
        Messages.send(player, "server.claim-info-owner", java.util.Map.of("owner", claim.getOwnerName()), false);
        if (!claim.getTrusted().isEmpty()) {
            Messages.send(player, "server.claim-info-trusted", java.util.Map.of("count", claim.getTrusted().size()), false);
        }
        if (claim.isForSale()) {
            Messages.send(player, "server.claim-info-for-sale", java.util.Map.of("price", MoneyFormat.format(claim.getSalePrice())), false);
        }
    }

    private void handleSettings(Player player) {
        var chunk = player.getLocation().getChunk();
        ClaimData claim = claimManager.getClaimResynced(chunk);
        if (claim == null) {
            sendMessage(player, "claim.not-claimed", "&cCe chunk n'est pas claim.");
            return;
        }
        if (!claim.getOwnerUuid().equals(player.getUniqueId()) && !player.hasPermission("crew.claim.admin")) {
            sendMessage(player, "claim.not-owner", "&cVous n'êtes pas le propriétaire de ce claim.");
            return;
        }
        claimSettingsGuiManager.open(player, chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    private void handleSee(Player player) {
        boolean nowActive = claimVisualizer.toggle(player);
        if (nowActive) {
            sendMessage(player, "claim.see-enabled", "&aAffichage des claims aux alentours activé.");
        } else {
            sendMessage(player, "claim.see-disabled", "&7Affichage des claims aux alentours désactivé.");
        }
    }

    private void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            Messages.send(player, "server.claim-d3a8a6c");
            return;
        }

        if (args[1].equalsIgnoreCase("cancel")) {
            ClaimManager.SellResult result = claimManager.cancelSale(player);
            switch (result) {
                case SUCCESS:
                    sendMessage(player, "claim.sale-cancelled", "&aCe claim n'est plus en vente.");
                    break;
                case NOT_CLAIMED:
                    sendMessage(player, "claim.not-claimed", "&cCe chunk n'est pas claim.");
                    break;
                case NOT_OWNER:
                    sendMessage(player, "claim.not-owner", "&cVous n'êtes pas le propriétaire de ce claim.");
                    break;
                case NOT_FOR_SALE:
                    sendMessage(player, "claim.not-for-sale", "&cCe claim n'est pas en vente.");
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
            Messages.send(player, "server.claim-d3a8a6cx");
            return;
        }

        ClaimManager.SellResult result = claimManager.sellClaim(player, price);
        switch (result) {
            case SUCCESS:
                String def = "&aCe claim est maintenant en vente pour &e{price}&a. Utilisez /claim sell cancel pour annuler.";
                String message = plugin.getMessages().getString("claim.sale-started", def)
                        .replace("{price}", MoneyFormat.format(price));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix", "") + message));
                break;
            case INVALID_PRICE:
                sendMessage(player, "claim.invalid-price", "&cLe prix doit être un nombre positif.");
                break;
            case NOT_CLAIMED:
                sendMessage(player, "claim.not-claimed", "&cCe chunk n'est pas claim.");
                break;
            case NOT_OWNER:
                sendMessage(player, "claim.not-owner", "&cVous n'êtes pas le propriétaire de ce claim.");
                break;
            default:
                break;
        }
    }

    private void handleBuy(Player player) {
        ClaimManager.BuyResult result = claimManager.buyClaim(player);
        switch (result) {
            case SUCCESS:
                sendMessage(player, "claim.buy-success", "&aVous avez acheté ce claim.");
                break;
            case NOT_CLAIMED:
                sendMessage(player, "claim.not-claimed", "&cCe chunk n'est pas claim.");
                break;
            case NOT_FOR_SALE:
                sendMessage(player, "claim.not-for-sale", "&cCe claim n'est pas en vente.");
                break;
            case OWN_CLAIM:
                sendMessage(player, "claim.own-claim", "&cVous êtes déjà le propriétaire de ce claim.");
                break;
            case NOT_ENOUGH_MONEY:
                sendMessage(player, "claim.not-enough-money", "&cVous n'avez pas assez d'argent pour acheter ce claim.");
                break;
            case LIMIT_REACHED:
                String def = "&cVous avez atteint votre nombre maximum de claims (&e{max}&c).";
                String message = plugin.getMessages().getString("claim.limit-reached-buy", def)
                        .replace("{max}", String.valueOf(claimManager.getMaxClaims(player)));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessages().getString("prefix", "") + message));
                break;
        }
    }

    private void sendMessage(Player player, String key, String def) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix", "") + plugin.getMessages().getString(key, def)));
        maybeHintNearbyOwnClaim(player, key);
    }

    /**
     * Après un message "ce chunk n'est pas claim", si le joueur possède en réalité un claim
     * juste à côté (cas fréquent : il a claim un chunk puis a un peu bougé avant de taper la
     * commande suivante), on l'aide à comprendre pourquoi au lieu de le laisser penser que
     * son claim a disparu.
     */
    private void maybeHintNearbyOwnClaim(Player player, String key) {
        if (!"claim.not-claimed".equals(key)) {
            return;
        }
        List<ClaimData> nearby = claimManager.getNearbyClaims(player.getLocation(), 1);
        boolean ownsAdjacent = nearby.stream().anyMatch(c -> c.getOwnerUuid().equals(player.getUniqueId()));
        if (ownsAdjacent) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&7Vous possédez un claim juste à côté : vous n'êtes plus exactement dessus. " +
                            "Faites /claim see pour voir ses limites et repositionnez-vous."));
        }
    }

    private void sendMessage(Player player, String rawMessage) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', rawMessage));
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
