package fr.crewcmoi.economie.managers;

import fr.crewcmoi.Main;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import fr.crewcmoi.utils.MoneyFormat;

/**
 * Affiche en permanence le solde du joueur dans l'action bar, avec la police
 * custom "dig2pic" (police de resource pack utilisée pour l'affichage de l'argent).
 */
public class ActionBarManager {

    // Namespace par défaut "minecraft" : si votre resource pack déclare la police
    // sous un autre namespace (ex: "crewcmoi:dig2pic"), changez cette clé en conséquence.
    private static final Key DIG2PIC_FONT = Key.key("dig2pic");

    private final Main plugin;
    private final EconomyManager economyManager;

    private BukkitTask task;

    public ActionBarManager(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    /**
     * Démarre la tâche répétitive qui met à jour l'action bar de tous les joueurs en ligne.
     */
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
        String currency = plugin.getConfig().getString("economy.currency-symbol", " \uE517");
        for (Player player : Bukkit.getOnlinePlayers()) {
            double balance = economyManager.getBalance(player.getUniqueId());
            String text = MoneyFormat.format(balance) + currency;
            Component component = Component.text(text);
            player.sendActionBar(component);
        }
    }
}
