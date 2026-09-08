package fr.crewcmoi.economie.managers;

import fr.crewcmoi.Main;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import fr.crewcmoi.other.utils.MoneyFormat;

public class ActionBarManager {

    
    private static final Key DIG2PIC_FONT = Key.key("dig2pic");

    private final Main plugin;
    private final EconomyManager economyManager;

    private BukkitTask task;

    public ActionBarManager(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        String currency = plugin.getConfig().getString("economy.currency-symbol");
        for (Player player : Bukkit.getOnlinePlayers()) {
            double balance = economyManager.getBalance(player.getUniqueId());
            String text = MoneyFormat.format(balance) + currency;
            Component component = Component.text(text);
            player.sendActionBar(component);
        }
    }
}
