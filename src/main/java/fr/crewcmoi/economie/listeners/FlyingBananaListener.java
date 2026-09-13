package fr.crewcmoi.economie.listeners;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class FlyingBananaListener implements Listener {
    private final Main plugin;

    public FlyingBananaListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        String configuredId = plugin.getConfig().getString("flying-banana.itemsadder-id", "").trim();
        if (configuredId.isEmpty()) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null || !configuredId.equalsIgnoreCase(customStack.getNamespacedID())) return;

        int durationTicks = plugin.getConfig().getInt("flying-banana.effect.duration-ticks", 20);
        int level = plugin.getConfig().getInt("flying-banana.effect.level", 25);
        int amplifier = Math.max(0, level - 1);

        event.getPlayer().addPotionEffect(
                new PotionEffect(PotionEffectType.LEVITATION, durationTicks, amplifier, false, true, true)
        );
    }
}
