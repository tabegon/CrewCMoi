package fr.crewcmoi.economie.listeners;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Récompense configurable lorsqu'un joueur tue un autre joueur avec le couteau ItemsAdder.
 * La mécanique est volontairement indépendante du système de lootbox.
 */
public class KnifeListener implements Listener {
    private final Main plugin;

    public KnifeListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        String knifeId = plugin.getConfig().getString("knife.itemsadder-id", "").trim();
        String soulId = plugin.getConfig().getString("knife.soul-itemsadder-id", "").trim();
        if (knifeId.isBlank() || soulId.isBlank()) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!isItemsAdderItem(weapon, knifeId)) return;

        CustomStack soul = CustomStack.getInstance(soulId);
        if (soul == null) {
            plugin.getLogger().warning("Knife : impossible de charger l'âme ItemsAdder '" + soulId + "'.");
            return;
        }

        killer.getInventory().addItem(soul.getItemStack().clone());

        String message = plugin.getConfig().getString(
                "knife.reward-message",
                "&5+1 âme &7pour avoir tué &f{victim}&7 avec le couteau !"
        );
        message = message.replace("{victim}", victim.getName());
        killer.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
    }

    private boolean isItemsAdderItem(ItemStack stack, String expectedId) {
        if (stack == null || stack.getType().isAir()) return false;
        CustomStack custom = CustomStack.byItemStack(stack);
        return custom != null && expectedId.equalsIgnoreCase(custom.getNamespacedID());
    }
}
