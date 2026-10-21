package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class PvpGroupManager implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final File file;
    private final YamlConfiguration data;

    public PvpGroupManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pvp-groups.yml");
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Impossible de créer pvp-groups.yml : " + exception.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public int getGroup(UUID uuid) {
        return Math.max(0, Math.min(2, data.getInt("players." + uuid, 1)));
    }

    public double getPunishmentAmount(UUID victimUuid) {
        int group = getGroup(victimUuid);
        return plugin.getConfig().getDouble("bounty.pvp-groups.amounts." + group,
                group == 0 ? 150.0 : group == 2 ? 50.0 : 100.0);
    }

    public boolean setGroup(UUID uuid, int group) {
        if (group < 0 || group > 2) return false;
        data.set("players." + uuid, group);
        try {
            data.save(file);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Impossible de sauvegarder le groupe PvP : " + exception.getMessage());
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crew.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Tu n'as pas la permission.");
            return true;
        }
        if (args.length != 2 || !args[1].matches("[012]")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage : /pvpgroup <joueur> <0|1|2>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null) {
            sender.sendMessage(ChatColor.RED + "Joueur introuvable.");
            return true;
        }
        int group = Integer.parseInt(args[1]);
        if (!setGroup(target.getUniqueId(), group)) {
            sender.sendMessage(ChatColor.RED + "Impossible d'enregistrer le groupe PvP.");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "Groupe PvP de " + target.getName() + " défini sur " + group
                + " (punition serveur : " + (group == 0 ? 150 : group == 1 ? 100 : 50) + ").");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        if (args.length == 2) return List.of("0", "1", "2").stream()
                .filter(value -> value.startsWith(args[1])).toList();
        return List.of();
    }
}
