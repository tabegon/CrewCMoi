package fr.crewcmoi.commands;

import fr.economy.Main;
import fr.economy.data.PlayerData;
import fr.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Commande /baltop : affiche le classement des joueurs les plus riches dans une GUI.
 */
public class BalanceTopCommand implements CommandExecutor {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final DecimalFormat format = new DecimalFormat("#,##0.00");

    public BalanceTopCommand(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
            return true;
        }

        Player player = (Player) sender;

        // La récupération en base doit se faire de façon asynchrone puis l'ouverture de l'inventaire de façon synchrone.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PlayerData> top = economyManager.getTopBalances(45);
            Bukkit.getScheduler().runTask(plugin, () -> openGui(player, top));
        });

        return true;
    }

    private void openGui(Player viewer, List<PlayerData> top) {
        int size = 54;
        Inventory gui = Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', "&6&lClassement des richesses"));

        String currency = plugin.getConfig().getString("economy.currency-symbol", "$");

        List<ItemStack> items = new ArrayList<>();
        int rank = 1;

        for (PlayerData data : top) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(data.getUuid()));

                String rankColor;
                if (rank == 1) rankColor = "&6";
                else if (rank == 2) rankColor = "&7";
                else if (rank == 3) rankColor = "&c";
                else rankColor = "&e";

                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                        rankColor + "&l#" + rank + " &f" + data.getName()));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&7Solde : &a" + format.format(data.getBalance()) + currency));
                meta.setLore(lore);

                skull.setItemMeta(meta);
            }

            items.add(skull);
            rank++;
        }

        for (int i = 0; i < items.size() && i < size; i++) {
            gui.setItem(i, items.get(i));
        }

        viewer.openInventory(gui);
    }
}
