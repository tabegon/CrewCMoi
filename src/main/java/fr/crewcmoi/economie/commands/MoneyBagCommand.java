package fr.crewcmoi.economie.commands;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.other.utils.MoneyFormat;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class MoneyBagCommand implements CommandExecutor {
    private static final String BAG_ID = "server:ccoin_bag";
    private final Main plugin;
    private final EconomyManager economy;
    private final NamespacedKey valueKey;

    public MoneyBagCommand(Main plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.valueKey = new NamespacedKey(plugin, "money_bag_value");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Cette commande doit être utilisée en jeu."); return true; }
        if (args.length != 1) { player.sendMessage(ChatColor.RED + "Usage : /moneybag <montant>"); return true; }
        double amount;
        try { amount = MoneyFormat.parseWhole(args[0]); }
        catch (NumberFormatException exception) { player.sendMessage(ChatColor.RED + "Montant invalide : utilisez un nombre entier sans virgule."); return true; }
        if (!Double.isFinite(amount) || amount <= 0) { player.sendMessage(ChatColor.RED + "Le montant doit être supérieur à 0."); return true; }
        CustomStack custom = CustomStack.getInstance(BAG_ID);
        if (custom == null || custom.getItemStack() == null) { player.sendMessage(ChatColor.RED + "L'item server:ccoin_bag est indisponible."); return true; }
        if (!economy.withdraw(player.getUniqueId(), amount)) { player.sendMessage(ChatColor.RED + "Tu n'as pas assez d'argent."); return true; }
        ItemStack bag = custom.getItemStack().clone();
        bag.setAmount(1);
        ItemMeta meta = bag.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.DOUBLE, amount);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + MoneyFormat.format(amount) + plugin.getConfig().getString("economy.currency-symbol", ""));
            lore.add(ChatColor.YELLOW + "Clic droit : poser le sac");
            lore.add(ChatColor.GOLD + "Shift + clic droit : empocher sa valeur");
            meta.setLore(lore);
            bag.setItemMeta(meta);
        }
        var leftovers = player.getInventory().addItem(bag);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage(ChatColor.GREEN + "Tu as placé " + MoneyFormat.format(amount) + plugin.getConfig().getString("economy.currency-symbol", "") + " dans un sac.");
        return true;
    }

    public NamespacedKey getValueKey() { return valueKey; }
}
