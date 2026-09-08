package fr.crewcmoi.economie.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.database.PlayerData;
import fr.crewcmoi.economie.gui.BaltopHolder;
import fr.crewcmoi.economie.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import fr.crewcmoi.other.utils.MoneyFormat;
import fr.crewcmoi.other.utils.GuiItems;
import java.util.ArrayList;
import java.util.List;

public class BalanceTopCommand implements CommandExecutor {

    private final Main plugin;
    private final EconomyManager economyManager;

    public BalanceTopCommand(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, "economy.balancetop.player-only");
            return true;
        }

        Player player = (Player) sender;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PlayerData> top = economyManager.getTopBalances(45);
            Bukkit.getScheduler().runTask(plugin, () -> openGui(player, top));
        });

        return true;
    }

    private void openGui(Player viewer, List<PlayerData> top) {
        int size = 54;
        BaltopHolder holder = new BaltopHolder();
        Inventory gui = Bukkit.createInventory(holder, size, ChatColor.translateAlternateColorCodes('&', "&6&lᴄʟᴀꜱꜱᴇᴍᴇɴᴛ ᴅᴇꜱ ʀɪᴄʜᴇꜱꜱᴇꜱ"));
        holder.setInventory(gui);

        String currency = plugin.getConfig().getString("economy.currency-symbol");

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
                        "&7ꜱᴏʟᴅᴇ : &a" + MoneyFormat.format(data.getBalance()) + "&f" + currency));
                meta.setLore(lore);

                skull.setItemMeta(meta);
            }

            items.add(skull);
            rank++;
        }

        for (int i = 0; i < items.size() && i < 45; i++) {
            gui.setItem(i, items.get(i));
        }

        ItemStack nothing = GuiItems.nothing(" ");
        for (int slot = 45; slot < size; slot++) {
            gui.setItem(slot, nothing);
        }

        viewer.openInventory(gui);
    }
}
