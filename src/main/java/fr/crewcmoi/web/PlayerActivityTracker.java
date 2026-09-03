package fr.crewcmoi.web;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerActivityTracker implements Listener {

    private static final int MAX_MESSAGES = 5;
    private static final int MAX_COMMANDS = 3;

    private final Map<UUID, LinkedList<String>> lastMessages = new ConcurrentHashMap<>();
    private final Map<UUID, LinkedList<String>> lastCommands = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();

    public void initializeOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sessionStartTimes.putIfAbsent(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        LinkedList<String> messages = lastMessages.computeIfAbsent(uuid, key -> new LinkedList<>());

        synchronized (messages) {
            messages.addLast(event.getMessage());
            while (messages.size() > MAX_MESSAGES) {
                messages.removeFirst();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        LinkedList<String> commands = lastCommands.computeIfAbsent(uuid, key -> new LinkedList<>());

        synchronized (commands) {
            commands.addLast(event.getMessage());
            while (commands.size() > MAX_COMMANDS) {
                commands.removeFirst();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        sessionStartTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        sessionStartTimes.remove(event.getPlayer().getUniqueId());
    }

    public List<String> getLastMessages(UUID uuid) {
        LinkedList<String> messages = lastMessages.get(uuid);
        if (messages == null) {
            return Collections.emptyList();
        }

        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    public List<String> getLastCommands(UUID uuid) {
        LinkedList<String> commands = lastCommands.get(uuid);
        if (commands == null) {
            return Collections.emptyList();
        }

        synchronized (commands) {
            return new ArrayList<>(commands);
        }
    }

    public long getSessionDurationMillis(UUID uuid) {
        Long start = sessionStartTimes.get(uuid);
        if (start == null) {
            return 0L;
        }
        return Math.max(0L, System.currentTimeMillis() - start);
    }

    public String getFormattedSessionDuration(UUID uuid) {
        long totalSeconds = getSessionDurationMillis(uuid) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
