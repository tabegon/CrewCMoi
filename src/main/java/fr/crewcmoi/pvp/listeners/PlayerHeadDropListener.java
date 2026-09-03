package fr.crewcmoi.pvp.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.MoneyFormat;
import fr.crewcmoi.pvp.managers.DuelManager;
import fr.crewcmoi.pvp.utils.HeadSellPrice;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Fait dropper la tête de la victime lors d'un kill JcJ "normal" (hors duel) : un item
 * PLAYER_HEAD à son effigie, avec un lore indiquant son prix de vente. Ce prix est aussi
 * tagué de façon sécurisée sur l'item (voir HeadSellPrice) pour qu'il reste vendable au
 * bon montant via /sell, même si le lore affiché ne fait plus foi (ex : renommage).
 */
public class PlayerHeadDropListener implements Listener {

    private final Main plugin;
    private final DuelManager duelManager;

    public PlayerHeadDropListener(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
    }

    // Priorité LOW : s'exécute avant DuelListener#onPlayerDeath (priorité par défaut),
    // pour être sûr de lire l'état "en duel" avant qu'il ne soit nettoyé.
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("player-head-drop.enabled", true)) {
            return;
        }

        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            // Seuls les kills JcJ par un autre joueur font dropper une tête (pas les morts
            // par mob, chute, environnement, etc.).
            return;
        }
        if (duelManager.getSession(victim.getUniqueId()) != null) {
            // Une mort en duel a son propre traitement (voir DuelListener : la tête ne
            // drop que si l'option "Tête du perdant" a été activée dans la GUI /duel).
            return;
        }

        double price = plugin.getConfig().getDouble("player-head-drop.price", 250.0);
        ItemStack head = createHead(plugin, victim, price);
        if (head != null) {
            victim.getWorld().dropItemNaturally(victim.getLocation(), head);
        }
    }

    /**
     * Construit l'item "tête de joueur" à l'effigie de la victime, avec son prix de
     * vente en lore et tagué de façon sécurisée (voir HeadSellPrice). Réutilisé par
     * DuelListener pour l'option "Tête du perdant" des duels.
     */
    public static ItemStack createHead(Main plugin, Player victim, double price) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return null;
        }

        meta.setOwningPlayer(victim);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fTête de &e" + victim.getName()));

        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&',
                "&7Prix de vente : &a" + MoneyFormat.format(price) + currency));
        meta.setLore(lore);

        HeadSellPrice.apply(plugin, meta, price);
        head.setItemMeta(meta);
        return head;
    }
}
