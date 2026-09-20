package fr.crewcmoi.pvp.managers;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.utils.DuelMessages;
import fr.crewcmoi.economie.managers.EconomyManager;
import fr.crewcmoi.other.utils.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    public static class DuelRequest {
        private final UUID requesterUuid;
        private final boolean keepInventory;
        private final double bet;
        private final boolean dropHead;
        private final String kitId;
        private final BukkitTask expiryTask;

        public DuelRequest(UUID requesterUuid, boolean keepInventory, double bet, boolean dropHead, String kitId, BukkitTask expiryTask) {
            this.requesterUuid = requesterUuid;
            this.keepInventory = keepInventory;
            this.bet = bet;
            this.dropHead = dropHead;
            this.kitId = kitId;
            this.expiryTask = expiryTask;
        }

        public UUID getRequesterUuid() {
            return requesterUuid;
        }

        public boolean isKeepInventory() {
            return keepInventory;
        }

        public double getBet() {
            return bet;
        }

        public boolean isDropHead() {
            return dropHead;
        }

        public String getKitId() {
            return kitId;
        }
    }

    private final Main plugin;
    private final EconomyManager economyManager;
    private final CombatManager combatManager;
    private DuelArenaManager duelArenaManager;
    private final int expirySeconds;
    private final int countdownSeconds;

    private final Map<UUID, DuelRequest> pendingRequests = new ConcurrentHashMap<>();

    
    private final Map<UUID, DuelSession> activeDuels = new ConcurrentHashMap<>();
    // Un seul duel à la fois par arène.
    private final Map<Integer, DuelSession> activeArenaDuels = new ConcurrentHashMap<>();

    // Joueurs protégés après un duel : ils ne peuvent pas accepter un nouveau duel
    // pendant la phase de récupération des objets.
    private final Map<UUID, Long> postDuelRecoveryExpiry = new ConcurrentHashMap<>();
    private final int postDuelRecoverySeconds;

    public DuelManager(Main plugin, EconomyManager economyManager, CombatManager combatManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.combatManager = combatManager;
        this.expirySeconds = plugin.getConfig().getInt("duel.expiry-seconds", 60);
        this.countdownSeconds = plugin.getConfig().getInt("duel.countdown-seconds", 5);
        this.postDuelRecoverySeconds = plugin.getConfig().getInt("duel.post-duel-recovery-seconds", 30);
    }

    public boolean isInDuel(UUID uuid) {
        return activeDuels.containsKey(uuid);
    }

    public boolean isInPostDuelRecovery(UUID uuid) {
        Long expiry = postDuelRecoveryExpiry.get(uuid);
        if (expiry == null) {
            return false;
        }
        if (expiry <= System.currentTimeMillis()) {
            postDuelRecoveryExpiry.remove(uuid, expiry);
            return false;
        }
        return true;
    }

    public int getPostDuelRecoveryRemainingSeconds(UUID uuid) {
        Long expiry = postDuelRecoveryExpiry.get(uuid);
        if (expiry == null) {
            return 0;
        }
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            postDuelRecoveryExpiry.remove(uuid, expiry);
            return 0;
        }
        return (int) Math.ceil(remaining / 1000.0);
    }

    private void startPostDuelRecovery(UUID uuid) {
        if (postDuelRecoverySeconds <= 0) {
            return;
        }
        long expiry = System.currentTimeMillis() + postDuelRecoverySeconds * 1000L;
        postDuelRecoveryExpiry.put(uuid, expiry);
        Bukkit.getScheduler().runTaskLater(plugin, () -> postDuelRecoveryExpiry.remove(uuid, expiry), postDuelRecoverySeconds * 20L);
    }

    public void setDuelArenaManager(DuelArenaManager duelArenaManager) {
        this.duelArenaManager = duelArenaManager;
    }

    public boolean hasPendingRequest(UUID targetUuid) {
        return pendingRequests.containsKey(targetUuid);
    }

    public DuelSession getSession(UUID uuid) {
        return activeDuels.get(uuid);
    }

    public void createRequest(Player requester, Player target, boolean keepInventory, double bet, boolean dropHead, String kitId) {
        UUID targetUuid = target.getUniqueId();
        cancelRequest(targetUuid);

        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelRequest current = pendingRequests.get(targetUuid);
            if (current != null && current.getRequesterUuid().equals(requester.getUniqueId())) {
                pendingRequests.remove(targetUuid);

                Player requesterPlayer = Bukkit.getPlayer(requester.getUniqueId());
                if (requesterPlayer != null && requesterPlayer.isOnline()) {
                    sendMessage(requesterPlayer, "duel.expired-requester", "{player}", target.getName());
                }
                if (target.isOnline()) {
                    sendMessage(target, "duel.expired-target", "{player}", requester.getName());
                }
            }
        }, expirySeconds * 20L);

        pendingRequests.put(targetUuid, new DuelRequest(requester.getUniqueId(), keepInventory, bet, dropHead, kitId, expiryTask));

        String betText = bet > 0 ? MoneyFormat.format(bet) : plugin.getMessages().getString("duel.no-bet");
        sendMessage(requester, "duel.sent", "{player}", target.getName());
        DuelMessages.sendRequestReceived(plugin, target, requester, keepInventory, betText, dropHead, kitId == null ? null : plugin.getDuelKitManager().getDisplayName(kitId), kitId == null ? 0.0 : plugin.getDuelKitManager().getPrice(kitId));
    }

    public void cancelRequest(UUID targetUuid) {
        DuelRequest existing = pendingRequests.remove(targetUuid);
        if (existing != null && existing.expiryTask != null) {
            existing.expiryTask.cancel();
        }
    }

    public void deny(Player target) {
        DuelRequest request = pendingRequests.remove(target.getUniqueId());
        if (request != null && request.expiryTask != null) {
            request.expiryTask.cancel();
        }
        if (request == null) {
            sendMessage(target, "duel.no-request", null, null);
            return;
        }
        Player requester = Bukkit.getPlayer(request.getRequesterUuid());
        sendMessage(target, "duel.denied-target", null, null);
        if (requester != null && requester.isOnline()) {
            sendMessage(requester, "duel.denied-requester", "{player}", target.getName());
        }
    }

    public void accept(Player target) {
        UUID targetUuid = target.getUniqueId();
        // Ne retirer la demande qu'après toutes les vérifications bloquantes.
        // Ainsi, une demande reste disponible si l'acceptation est refusée
        // temporairement (combat / duel / récupération).
        DuelRequest request = pendingRequests.get(targetUuid);

        if (request == null) {
            sendMessage(target, "duel.no-request", null, null);
            return;
        }

        Player requester = Bukkit.getPlayer(request.getRequesterUuid());
        if (requester == null || !requester.isOnline()) {
            sendMessage(target, "duel.requester-offline", null, null);
            return;
        }

        if (isInDuel(target.getUniqueId()) || isInDuel(requester.getUniqueId())) {
            sendMessage(target, "duel.already-in-duel", null, null);
            return;
        }

        if (combatManager != null && (combatManager.isInCombat(target) || combatManager.isInCombat(requester))) {
            sendMessage(target, "duel.in-combat", null, null);
            sendMessage(requester, "duel.in-combat", null, null);
            return;
        }

        if (isInPostDuelRecovery(target.getUniqueId()) || isInPostDuelRecovery(requester.getUniqueId())) {
            int targetRemaining = getPostDuelRecoveryRemainingSeconds(target.getUniqueId());
            int requesterRemaining = getPostDuelRecoveryRemainingSeconds(requester.getUniqueId());
            int remaining = Math.max(targetRemaining, requesterRemaining);
            sendMessage(target, "duel.post-duel-recovery", "{seconds}", String.valueOf(remaining));
            sendMessage(requester, "duel.post-duel-recovery", "{seconds}", String.valueOf(remaining));
            return;
        }

        // Toutes les vérifications sont passées : la demande peut maintenant être consommée.
        request = pendingRequests.remove(targetUuid);
        if (request == null) {
            sendMessage(target, "duel.no-request", null, null);
            return;
        }
        if (request.expiryTask != null) {
            request.expiryTask.cancel();
        }

        double bet = request.getBet();
        String kitId = request.getKitId();
        double kitPrice = kitId != null ? plugin.getConfig().getDouble("duel.kits.basic.price", 1000.0) : 0.0;

        if (kitId != null && (kitId.isBlank() || !plugin.getDuelKitManager().isValidKit(kitId))) {
            sendMessage(target, "duel.invalid-kit", null, null);
            sendMessage(requester, "duel.invalid-kit", null, null);
            return;
        }

        double totalCost = bet + kitPrice;
        if (totalCost > 0) {
            if (!economyManager.has(requester.getUniqueId(), totalCost)) {
                sendMessage(requester, "duel.self-not-enough-money", null, null);
                sendMessage(target, "duel.requester-not-enough-money", "{player}", requester.getName());
                return;
            }
            if (!economyManager.has(target.getUniqueId(), totalCost)) {
                sendMessage(target, "duel.self-not-enough-money", null, null);
                sendMessage(requester, "duel.target-not-enough-money", "{player}", target.getName());
                return;
            }
        }

        if (bet > 0) {
            if (!economyManager.has(requester.getUniqueId(), bet)) {
                sendMessage(target, "duel.requester-not-enough-money", "{player}", requester.getName());
                sendMessage(requester, "duel.self-not-enough-money", null, null);
                return;
            }
            if (!economyManager.has(target.getUniqueId(), bet)) {
                sendMessage(target, "duel.self-not-enough-money", null, null);
                sendMessage(requester, "duel.target-not-enough-money", "{player}", target.getName());
                return;
            }
            economyManager.withdraw(requester.getUniqueId(), bet);
            economyManager.withdraw(target.getUniqueId(), bet);
        }

        // Cherche la première arène configurée et libre.
        // Arène 1 = duel.arena.pos1 / pos2
        // Arène 2 = duel.arena.second.pos1 / pos2
        int arenaIndex = findAvailableArenaIndex();
        if (arenaIndex == -1) {
            sendMessage(target, "duel.arena-occupied", null, null);
            sendMessage(requester, "duel.arena-occupied", null, null);
            // La mise a déjà été retirée juste avant la recherche d'arène.
            // On la rend donc immédiatement si aucune arène n'est disponible.
            if (bet > 0) {
                economyManager.deposit(requester.getUniqueId(), bet);
                economyManager.deposit(target.getUniqueId(), bet);
            }
            return;
        }

        Location arena1 = getArenaLocation(arenaIndex, 1);
        Location arena2 = getArenaLocation(arenaIndex, 2);
        if (arena1 == null || arena2 == null) {
            sendMessage(target, "duel.arena-not-configured", null, null);
            sendMessage(requester, "duel.arena-not-configured", null, null);
            return;
        }

        if (kitPrice > 0) {
            economyManager.withdraw(requester.getUniqueId(), kitPrice);
            economyManager.withdraw(target.getUniqueId(), kitPrice);
        }

        DuelSession session = new DuelSession(requester.getUniqueId(), target.getUniqueId(),
                kitId == null && request.isKeepInventory(), bet, request.isDropHead(), kitId);
        session.setArenaIndex(arenaIndex);
        activeArenaDuels.put(arenaIndex, session);
        session.setOriginLocation1(requester.getLocation().clone());
        session.setOriginLocation2(target.getLocation().clone());

        if (session.hasKit()) {
            session.saveInventory(requester.getUniqueId(), requester);
            session.saveInventory(target.getUniqueId(), target);
            plugin.getDuelKitManager().applyKit(requester, kitId);
            plugin.getDuelKitManager().applyKit(target, kitId);
        }

        activeDuels.put(requester.getUniqueId(), session);
        activeDuels.put(target.getUniqueId(), session);

        requester.teleport(arena1);
        target.teleport(arena2);

        requester.setHealth(requester.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        requester.setFoodLevel(20);
        target.setHealth(target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        target.setFoodLevel(20);

        sendMessage(requester, "duel.accepted-requester", "{player}", target.getName());
        sendMessage(target, "duel.accepted-target", "{player}", requester.getName());

        startCountdown(requester, target, session);
    }

    private void startCountdown(Player p1, Player p2, DuelSession session) {
        final int[] remaining = {countdownSeconds};
        BukkitTask[] taskHolder = new BukkitTask[1];
        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player player1 = Bukkit.getPlayer(session.getPlayer1());
            Player player2 = Bukkit.getPlayer(session.getPlayer2());

            if (player1 == null || !player1.isOnline() || player2 == null || !player2.isOnline()
                    || !isInDuel(session.getPlayer1())) {
                taskHolder[0].cancel();
                return;
            }

            if (remaining[0] <= 0) {
                session.setStarted(true);
                sendMessage(player1, "duel.fight", null, null);
                sendMessage(player2, "duel.fight", null, null);
                taskHolder[0].cancel();
                return;
            }

            sendMessage(player1, "duel.countdown", "{seconds}", String.valueOf(remaining[0]));
            sendMessage(player2, "duel.countdown", "{seconds}", String.valueOf(remaining[0]));
            remaining[0]--;
        }, 0L, 20L);
    }

    public boolean handleDeath(Player victim) {
        DuelSession session = activeDuels.get(victim.getUniqueId());
        if (session == null) {
            return false;
        }

        UUID winnerUuid = session.getOpponent(victim.getUniqueId());
        Player winner = winnerUuid != null ? Bukkit.getPlayer(winnerUuid) : null;

        endSession(session, winner, victim, false);
        return true;
    }

    public void handleQuit(Player player) {
        cancelRequest(player.getUniqueId());

        DuelSession session = activeDuels.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        UUID winnerUuid = session.getOpponent(player.getUniqueId());
        Player winner = winnerUuid != null ? Bukkit.getPlayer(winnerUuid) : null;
        endSession(session, winner, player, true);
    }

    private void endSession(DuelSession session, Player winner, Player loser, boolean forfeit) {
        activeDuels.remove(session.getPlayer1());
        activeDuels.remove(session.getPlayer2());
        activeArenaDuels.remove(session.getArenaIndex(), session);

        // Les deux participants entrent en récupération pendant 30 secondes
        // (configurable), même si le duel s'est terminé par abandon.
        startPostDuelRecovery(session.getPlayer1());
        startPostDuelRecovery(session.getPlayer2());

        double pot = session.getBet() * 2;
        if (winner != null && winner.isOnline()) {
            if (pot > 0) {
                economyManager.deposit(winner.getUniqueId(), pot);
            }
            String key = forfeit ? "duel.won-forfeit" : "duel.won";
            sendMessage(winner, key, "{player}", loser.getName());
            if (pot > 0) {
                sendMessage(winner, "duel.won-pot", "{amount}", MoneyFormat.format(pot));
            }

            Location origin = session.getOriginLocation(winner.getUniqueId());
            if (origin != null) {
                if (session.isKeepInventory() || forfeit) {

                    winner.teleport(origin);
                } else {

                    sendMessage(winner, "duel.won-loot-time", null, null);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (winner.isOnline()) {
                            winner.teleport(origin);
                            sendMessage(winner, "duel.won-loot-teleport", null, null);
                        }
                    }, 30L * 20L);
                }
            }
        }

        if (session.hasKit()) {
            if (winner != null && winner.isOnline()) {
                session.restoreInventory(winner.getUniqueId(), winner);
            }
            if (loser != null && loser.isOnline()) {

                
                session.restoreInventory(loser.getUniqueId(), loser);
            }
        }

        
        if (loser != null && loser.isOnline() && !forfeit) {
            Location origin = session.getOriginLocation(loser.getUniqueId());
            if (origin != null) {

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (loser.isOnline()) {
                        loser.teleport(origin);
                    }
                }, 5L);
            }
            if (winner != null) {
                sendMessage(loser, "duel.lost", "{player}", winner.getName());
            }
        }

        combatManager.clearCombat(loser);
        if (winner != null) {
            combatManager.clearCombat(winner);
        }

        if (duelArenaManager != null) {
            // Le gestionnaire historique sait restaurer l'arène principale.
            // La seconde arène est destinée à être configurée séparément si besoin.
            if (session.getArenaIndex() == 1) {
                duelArenaManager.resetArena();
            }
        }
    }

    private int findAvailableArenaIndex() {
        for (int arenaIndex = 1; arenaIndex <= 2; arenaIndex++) {
            if (activeArenaDuels.containsKey(arenaIndex)) {
                continue;
            }
            if (getArenaLocation(arenaIndex, 1) != null && getArenaLocation(arenaIndex, 2) != null) {
                return arenaIndex;
            }
        }
        return -1;
    }

    private Location getArenaLocation(int arenaIndex, int position) {
        String path = arenaIndex == 1
                ? "duel.arena.pos" + position
                : "duel.arena.second.pos" + position;
        String worldName = plugin.getConfig().getString(path + ".world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = plugin.getConfig().getDouble(path + ".x");
        double y = plugin.getConfig().getDouble(path + ".y");
        double z = plugin.getConfig().getDouble(path + ".z");
        float yaw = (float) plugin.getConfig().getDouble(path + ".yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public DuelKitManager getDuelKitManager() {
        return plugin.getDuelKitManager();
    }

    public double getBetStep() {
        return plugin.getConfig().getDouble("duel.bet-step", 100.0);
    }

    public double getMaxBet() {
        return plugin.getConfig().getDouble("duel.max-bet", 0.0);
    }

    private void sendMessage(Player player, String path, String placeholder, String value) {
        if (player == null || !player.isOnline()) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix");
        String message = plugin.getMessages().getString(path, "");
        if (message.isEmpty()) {
            return;
        }
        if (placeholder != null) {
            message = message.replace(placeholder, value);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
