package fr.crewcmoi.economie.listeners;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlyingBananaListener implements Listener {
    private final Main plugin;

    /**
     * Joueurs qui viennent de manger une Flying Banana.
     * La protection reste active jusqu'à l'atterrissage, avec une durée maximale
     * de sécurité pour éviter qu'un joueur reste protégé indéfiniment.
     */
    private final Map<UUID, BananaFlight> activeFlights = new HashMap<>();

    public FlyingBananaListener(Main plugin) {
        this.plugin = plugin;
        startFlightProtectionTask();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        String configuredId = plugin.getConfig().getString("flying-banana.itemsadder-id", "").trim();
        if (configuredId.isEmpty()) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null || !configuredId.equalsIgnoreCase(customStack.getNamespacedID())) return;

        Player player = event.getPlayer();

        int durationTicks = plugin.getConfig().getInt("flying-banana.effect.duration-ticks", 20);
        int level = plugin.getConfig().getInt("flying-banana.effect.level", 25);
        int amplifier = Math.max(0, level - 1);

        player.addPotionEffect(
                new PotionEffect(PotionEffectType.LEVITATION, durationTicks, amplifier, false, true, true)
        );

        // On protège la chute provoquée par la Flying Banana.
        // On attend que le joueur ait réellement quitté le sol avant
        // de considérer qu'il est en "vol".
        activeFlights.put(player.getUniqueId(), new BananaFlight(player));
        player.setFallDistance(0.0f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getCause() == EntityDamageEvent.DamageCause.FALL
                && activeFlights.containsKey(player.getUniqueId())) {

            // La rechute de la Flying Banana ne doit jamais infliger de dégâts.
            event.setCancelled(true);
            player.setFallDistance(0.0f);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeFlights.remove(event.getPlayer().getUniqueId());
    }

    private void startFlightProtectionTask() {
        // Vérification toutes les ticks pour enlever la protection dès
        // que le joueur a terminé sa chute.
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (activeFlights.isEmpty()) return;

            activeFlights.entrySet().removeIf(entry -> {
                BananaFlight flight = entry.getValue();
                Player player = flight.player;

                if (!player.isOnline() || player.isDead()) {
                    return true;
                }

                // Empêche Minecraft d'accumuler de la distance de chute
                // pendant la phase de vol/chute.
                player.setFallDistance(0.0f);

                if (!flight.leftGround && !player.isOnGround()) {
                    flight.leftGround = true;
                }

                // Dès que le joueur a réellement quitté le sol puis
                // atterrit, la protection est terminée.
                if (flight.leftGround && player.isOnGround()) {
                    player.setFallDistance(0.0f);
                    return true;
                }

                // Sécurité : ne jamais garder la protection indéfiniment.
                if (++flight.ticks > 20 * 15) { // 15 secondes maximum
                    return true;
                }

                return false;
            });
        }, 1L, 1L);
    }

    private static final class BananaFlight {
        private final Player player;
        private boolean leftGround;
        private int ticks;

        private BananaFlight(Player player) {
            this.player = player;
        }
    }
}
