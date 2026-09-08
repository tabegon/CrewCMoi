package fr.crewcmoi.moderation.managers;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final Main plugin;
    private final Set<UUID> vanished = new HashSet<>();

    public VanishManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public Set<UUID> getVanishedPlayers() {
        return new HashSet<>(vanished);
    }

    public boolean toggle(Player player) {
        boolean newState = !isVanished(player.getUniqueId());
        setVanished(player, newState);
        return newState;
    }

    public void setVanished(Player player, boolean vanish) {
        UUID uuid = player.getUniqueId();

        if (vanish) {
            if (!vanished.add(uuid)) {
                return;
            }
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.equals(player)) {
                    online.hidePlayer(plugin, player);
                }
            }
        } else {
            if (!vanished.remove(uuid)) {
                return;
            }
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
        }
    }

    public void forceUnvanish(Player player) {
        if (isVanished(player.getUniqueId())) {
            setVanished(player, false);
        }
    }

    public void applyToJoiningPlayer(Player joining) {
        for (UUID uuid : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer != null && !vanishedPlayer.equals(joining)) {
                joining.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    public void forget(UUID uuid) {
        vanished.remove(uuid);
    }
}
