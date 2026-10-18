package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpectateManager implements Listener {
    private record State(Location location, GameMode gameMode, boolean invulnerable, UUID target) {}
    private final Main plugin;
    private final DuelManager duelManager;
    private final Map<UUID, State> spectators = new ConcurrentHashMap<>();
    private final BukkitTask followTask;

    public SpectateManager(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
        this.followTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public boolean isSpectating(UUID uuid) { return spectators.containsKey(uuid); }

    public boolean start(Player spectator, Player target) {
        if (!duelManager.isInDuel(target.getUniqueId())) {
            spectator.sendMessage(ChatColor.RED + target.getName() + " n'est pas en duel.");
            return false;
        }
        if (duelManager.isInDuel(spectator.getUniqueId())) {
            spectator.sendMessage(ChatColor.RED + "Tu ne peux pas observer un duel pendant que tu participes toi-même à un duel.");
            return false;
        }
        if (spectator.getUniqueId().equals(target.getUniqueId())) {
            spectator.sendMessage(ChatColor.RED + "Tu ne peux pas te spectater toi-même.");
            return false;
        }
        stop(spectator, false);
        spectators.put(spectator.getUniqueId(), new State(spectator.getLocation().clone(), spectator.getGameMode(), spectator.isInvulnerable(), target.getUniqueId()));
        spectator.setGameMode(GameMode.ADVENTURE);
        spectator.setInvulnerable(true);
        spectator.setFallDistance(0);
        follow(spectator, target);
        spectator.sendMessage(ChatColor.GREEN + "Tu observes maintenant le duel de " + target.getName() + ". Utilise /spectate stop pour quitter.");
        return true;
    }

    public void stop(Player player, boolean notify) {
        State state = spectators.remove(player.getUniqueId());
        if (state == null) return;
        player.setInvulnerable(state.invulnerable());
        player.setGameMode(state.gameMode());
        if (state.location().getWorld() != null) player.teleport(state.location());
        if (notify) player.sendMessage(ChatColor.YELLOW + "Tu as quitté le mode spectateur.");
    }

    public void shutdown() {
        followTask.cancel();
        for (UUID uuid : spectators.keySet().toArray(UUID[]::new)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) stop(player, false);
        }
    }

    private void tick() {
        for (Map.Entry<UUID, State> entry : spectators.entrySet()) {
            Player spectator = Bukkit.getPlayer(entry.getKey());
            Player target = Bukkit.getPlayer(entry.getValue().target());
            if (spectator == null || !spectator.isOnline()) continue;
            if (target == null || !target.isOnline() || !duelManager.isInDuel(target.getUniqueId())) {
                stop(spectator, true);
                continue;
            }
            follow(spectator, target);
        }
    }

    private void follow(Player spectator, Player target) {
        Location targetLocation = target.getLocation();
        Location desired = targetLocation.clone().subtract(targetLocation.getDirection().normalize().multiply(2.0)).add(0, 1.0, 0);
        desired.setDirection(targetLocation.clone().add(0, 1.0, 0).toVector().subtract(desired.toVector()));
        if (desired.getWorld() != null && !desired.getWorld().equals(spectator.getWorld())) spectator.teleport(desired);
        else if (spectator.getLocation().distanceSquared(desired) > 1.0 || Math.abs(spectator.getLocation().getYaw() - desired.getYaw()) > 20) spectator.teleport(desired);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (isSpectating(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isSpectating(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player && isSpectating(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isSpectating(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && isSpectating(victim.getUniqueId())) { event.setCancelled(true); return; }
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker != null && isSpectating(attacker.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        State state = spectators.get(event.getPlayer().getUniqueId());
        if (state != null) Bukkit.getScheduler().runTask(plugin, () -> stop(event.getPlayer(), false));
    }

    private Player resolvePlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }
}
