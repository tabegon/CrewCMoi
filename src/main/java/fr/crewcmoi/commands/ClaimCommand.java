package fr.crewcmoi.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.ClaimData;
import fr.crewcmoi.gui.ClaimSettingsGuiManager;
import fr.crewcmoi.managers.ClaimManager;
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
 */
public class ClaimCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ClaimManager claimManager;
    private final ClaimSettingsGuiManager claimSettingsGuiManager;

    public ClaimCommand(Main plugin, ClaimManager claimManager, ClaimSettingsGuiManager claimSettingsGuiManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.claimSettingsGuiManager = claimSettingsGuiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
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
                    sendMessage(player, "&cUsage: /claim trust <joueur>");
                    return true;
                }
                handleTrust(player, args[1], true);
                break;
            case "untrust":
                if (args.length < 2) {
                    sendMessage(player, "&cUsage: /claim untrust <joueur>");
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
            default:
                sendMessage(player, "&cUsage: /claim [unclaim|trust <joueur>|untrust <joueur>|info|settings]");
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
                sendMessage(player, "claim.limit-reached", "&cVous avez atteint votre nombre maximum de claims.");
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
        ClaimData claim = claimManager.getClaim(chunk);
        if (claim == null) {
            sendMessage(player, "claim.not-claimed", "&cCe chunk n'est pas claim.");
            return;
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7Ce chunk appartient à &e" + claim.getOwnerName() + "&7."));
        if (!claim.getTrusted().isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Joueurs de confiance : &e" +
                    claim.getTrusted().size() + " joueur(s)."));
        }
    }

    private void handleSettings(Player player) {
        var chunk = player.getLocation().getChunk();
        ClaimData claim = claimManager.getClaim(chunk);
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

    private void sendMessage(Player player, String key, String def) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix", "") + plugin.getMessages().getString(key, def)));
    }

    private void sendMessage(Player player, String rawMessage) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', rawMessage));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("unclaim", "trust", "untrust", "info", "settings");
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
        return new ArrayList<>();
    }
}
