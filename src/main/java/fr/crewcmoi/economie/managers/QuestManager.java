package fr.crewcmoi.economie.managers;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

/**
 * Gère la progression persistante de la première quête du Quest Master.
 *
 * La quête ne dépend d'aucune recherche de biome : les coordonnées des
 * montagnes sont déjà définies par seed dans QuestWorldManager.
 */
public final class QuestManager {
    private final Main plugin;
    private final NamespacedKey completedKey;
    private final NamespacedKey goatKey;
    private final NamespacedKey goatOwnerKey;
    private final NamespacedKey quest2CompletedKey;
    private final Map<UUID, UUID> activeGoats = new HashMap<>();
    private BukkitTask compassTask;

    public QuestManager(Main plugin) {
        this.plugin = plugin;
        this.completedKey = new NamespacedKey(plugin, "quest_1_completed");
        this.goatKey = new NamespacedKey(plugin, "quest_1_electric_goat");
        this.goatOwnerKey = new NamespacedKey(plugin, "quest_1_owner");
        this.quest2CompletedKey = new NamespacedKey(plugin, "quest_2_completed");
        startCompassTask();
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshNpcVisibilityForOnlinePlayers, 20L);
    }

    public boolean isCompleted(Player player) {
        return player.getPersistentDataContainer().getOrDefault(
                completedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
    }

    public boolean isQuest2Completed(Player player) {
        return player.getPersistentDataContainer().getOrDefault(
                quest2CompletedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
    }

    /** Lance la deuxième quête après validation de la première. */
    public void startQuest2(Player player) {
        if (!isCompleted(player)) {
            player.sendMessage(color(plugin.getConfig().getString(
                    "quest-master.quest-2.messages.locked",
                    "&cTu dois terminer la première quête avant de commencer celle-ci.")));
            return;
        }

        if (isQuest2Completed(player)) {
            player.sendMessage(color(plugin.getConfig().getString(
                    "quest-master.quest-2.messages.already-completed",
                    "&aTu as déjà terminé cette quête.")));
            return;
        }

        Location location = plugin.getQuestWorldManager().getRandomLocation();
        if (location == null) {
            player.sendMessage(color(plugin.getConfig().getString(
                    "quest-master.quest-2.messages.world-unavailable",
                    "&cLe monde quest n'est pas disponible.")));
            return;
        }

        player.closeInventory();
        player.teleport(location);
        player.sendMessage(color(plugin.getConfig().getString(
                "quest-master.quest-2.messages.started",
                "&eLa deuxième quête commence ! &7Résous l'énigme et rapporte l'objet au Quest Master.")));
    }

    /** Valide la deuxième quête lorsque le joueur présente l'objet final. */
    public boolean turnInQuest2Item(Player player) {
        if (!isCompleted(player) || isQuest2Completed(player)) return false;
        if (!isQuest2Item(player.getInventory().getItemInMainHand())) return false;

        ItemStack hand = player.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        }

        player.getPersistentDataContainer().set(quest2CompletedKey, PersistentDataType.BYTE, (byte) 1);
        player.sendMessage(color(plugin.getConfig().getString(
                "quest-master.quest-2.messages.completed",
                "&aQuête terminée ! &7Le NPC des cosmétiques est maintenant accessible.")));
        showCosmeticsNpc(player);
        executeFirstCosmeticCommands(player);
        return true;
    }

    public boolean isQuest2Item(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String itemsAdderId = plugin.getConfig().getString(
                "quest-master.quest-2.reward.itemsadder-id", "").trim();
        if (!itemsAdderId.isEmpty()) {
            CustomStack stack = CustomStack.byItemStack(item);
            return stack != null && itemsAdderId.equalsIgnoreCase(stack.getNamespacedID());
        }
        String material = plugin.getConfig().getString(
                "quest-master.quest-2.reward.material", "PAPER");
        return item.getType().name().equalsIgnoreCase(material);
    }

    /**
     * Débloque le premier cosmétique dans CosmeticsCore.
     *
     * CosmeticsCore considère les cosmétiques comme possédés lorsqu'un joueur
     * possède les permissions seeingui/wear correspondantes. On utilise donc
     * LuckPerms via la console afin de ne pas dépendre de son API Java.
     * Des commandes supplémentaires restent configurables pour les besoins
     * particuliers du serveur.
     */
    private void executeFirstCosmeticCommands(Player player) {
        String cosmeticId = plugin.getConfig().getString(
                "quest-master.quest-2.first-cosmetic.id", "").trim();

        boolean grantPermissions = plugin.getConfig().getBoolean(
                "quest-master.quest-2.first-cosmetic.grant-permissions", true);

        if (grantPermissions && !cosmeticId.isEmpty()) {
            for (String template : plugin.getConfig().getStringList(
                    "quest-master.quest-2.first-cosmetic.permission-commands")) {
                if (template == null || template.trim().isEmpty()) continue;
                String resolved = template
                        .replace("%player%", player.getName())
                        .replace("%uuid%", player.getUniqueId().toString())
                        .replace("%cosmetic%", cosmeticId);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
            }
        }

        // Commandes optionnelles supplémentaires (console), totalement configurables.
        for (String command : plugin.getConfig().getStringList(
                "quest-master.quest-2.first-cosmetic.commands")) {
            if (command == null || command.trim().isEmpty()) continue;
            String resolved = command.replace("%player%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString())
                    .replace("%cosmetic%", cosmeticId);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }
    }

    public void startQuest(Player player) {
        if (isCompleted(player)) {
            player.sendMessage(color(plugin.getConfig().getString("quest-master.quest-1.messages.already-completed", "&aQuête déjà terminée.")));
            return;
        }

        Location mountain = plugin.getQuestWorldManager().getRandomMountain();
        if (mountain == null) {
            player.sendMessage(color("&cAucune montagne n'est configurée pour la seed actuelle du monde quest."));
            return;
        }

        Entity existingGoat = findExistingGoat(player.getUniqueId());
        if (existingGoat != null) {
            activeGoats.put(player.getUniqueId(), existingGoat.getUniqueId());
            player.closeInventory();
            player.teleport(existingGoat.getLocation().clone().add(0, 0, 3));
            trackGoat(player, existingGoat);
            return;
        }

        player.closeInventory();
        player.teleport(mountain);
        spawnGoat(player, mountain);
    }

    private void spawnGoat(Player player, Location location) {
        String mythicMob = plugin.getConfig().getString("quest-master.quest-1.mythic-mob", "electric_goat").trim();
        if (mythicMob.isEmpty()) {
            player.sendMessage(color("&cLe MythicMob de la quête n'est pas configuré."));
            return;
        }

        Set<UUID> before = new HashSet<>();
        for (Entity entity : location.getWorld().getNearbyEntities(location, 6, 6, 6)) {
            before.add(entity.getUniqueId());
        }

        String world = location.getWorld().getName();
        String command = "mm mobs spawn -s " + mythicMob + ":1 1 "
                + world + "," + location.getX() + "," + location.getY() + "," + location.getZ();

        if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
            player.sendMessage(color("&cImpossible de faire apparaître l'Electric Goat."));
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Entity goat = findNewEntity(location, before);
            if (goat == null) {
                player.sendMessage(color("&cL'Electric Goat n'a pas pu être identifié."));
                return;
            }

            goat.getPersistentDataContainer().set(goatKey, PersistentDataType.BYTE, (byte) 1);
            goat.getPersistentDataContainer().set(goatOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());
            activeGoats.put(player.getUniqueId(), goat.getUniqueId());
            trackGoat(player, goat);
        }, 2L);
    }

    private Entity findExistingGoat(UUID owner) {
        String ownerId = owner.toString();
        org.bukkit.World world = plugin.getQuestWorldManager().getQuestWorld();
        if (world == null) return null;

        for (Entity entity : world.getEntities()) {
            String taggedOwner = entity.getPersistentDataContainer().get(goatOwnerKey, PersistentDataType.STRING);
            if (ownerId.equals(taggedOwner) && entity.isValid() && !entity.isDead()) {
                return entity;
            }
        }
        return null;
    }

    private Entity findNewEntity(Location location, Set<UUID> before) {
        Entity closest = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : location.getWorld().getNearbyEntities(location, 5, 5, 5)) {
            if (!entity.isValid() || entity instanceof Player) continue;
            if (before.contains(entity.getUniqueId())) continue;
            if (entity.getPersistentDataContainer().has(goatKey, PersistentDataType.BYTE)) continue;
            double distance = entity.getLocation().distanceSquared(location);
            if (distance < best) {
                best = distance;
                closest = entity;
            }
        }
        return closest;
    }

    private void trackGoat(Player player, Entity goat) {
        player.setCompassTarget(goat.getLocation());
        player.sendMessage(color(plugin.getConfig().getString("quest-master.quest-1.messages.started", "&eLa traque de l'Electric Goat commence !")));
    }

    /** Appelé lorsqu'un Electric Goat créé par cette quête meurt. */
    public void handleGoatDeath(Entity goat, Player killer) {
        String ownerValue = goat.getPersistentDataContainer().get(goatOwnerKey, PersistentDataType.STRING);
        if (ownerValue == null) return;

        UUID owner;
        try {
            owner = UUID.fromString(ownerValue);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        activeGoats.remove(owner);
        Player questPlayer = Bukkit.getPlayer(owner);
        if (questPlayer == null) return;

        if (killer == null || !killer.getUniqueId().equals(owner)) {
            questPlayer.sendMessage(color(plugin.getConfig().getString(
                    "quest-master.quest-1.messages.wrong-killer",
                    "&cTu dois battre toi-même l'Electric Goat pour obtenir sa corne.")));
            return;
        }

        ItemStack horn = createHorn();
        if (horn == null) {
            questPlayer.sendMessage(color("&cLa récompense de quête n'est pas correctement configurée."));
            return;
        }

        questPlayer.getInventory().addItem(horn);
        questPlayer.sendMessage(color(plugin.getConfig().getString(
                "quest-master.quest-1.messages.horn-obtained",
                "&aTu as récupéré la corne de l'Electric Goat ! Retourne voir le Quest Master.")));
    }

    private ItemStack createHorn() {
        String itemsAdderId = plugin.getConfig().getString("quest-master.quest-1.horn.itemsadder-id", "").trim();
        if (!itemsAdderId.isEmpty()) {
            CustomStack stack = CustomStack.getInstance(itemsAdderId);
            return stack == null ? null : stack.getItemStack().clone();
        }
        String material = plugin.getConfig().getString("quest-master.quest-1.horn.material", "GOAT_HORN");
        try {
            return new ItemStack(org.bukkit.Material.valueOf(material));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /** Valide la quête lorsque le joueur présente l'objet configuré au Quest Master. */
    public boolean turnInHorn(Player player) {
        if (isCompleted(player) || !isHorn(player.getInventory().getItemInMainHand())) return false;

        ItemStack hand = player.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        }

        player.getPersistentDataContainer().set(completedKey, PersistentDataType.BYTE, (byte) 1);
        player.sendMessage(color(plugin.getConfig().getString(
                "quest-master.quest-1.messages.completed",
                "&aQuête terminée ! Le SellBot est maintenant accessible.")));
        showSellBot(player);
        return true;
    }

    public boolean isHorn(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String itemsAdderId = plugin.getConfig().getString("quest-master.quest-1.horn.itemsadder-id", "").trim();
        if (!itemsAdderId.isEmpty()) {
            CustomStack stack = CustomStack.byItemStack(item);
            return stack != null && itemsAdderId.equalsIgnoreCase(stack.getNamespacedID());
        }
        String material = plugin.getConfig().getString("quest-master.quest-1.horn.material", "GOAT_HORN");
        return item.getType().name().equalsIgnoreCase(material);
    }

    public void refreshSellBotVisibility(Player player) {
        refreshNpcVisibility(player, "sell", isCompleted(player));
    }

    public void refreshCosmeticsNpcVisibility(Player player) {
        refreshNpcVisibility(player, "cosmetics", isQuest2Completed(player));
    }

    private void refreshNpcVisibility(Player player, String npcKey, boolean unlocked) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) return;
        int npcId = plugin.getConfig().getInt("npcs." + npcKey, 0);
        if (npcId <= 0) return;
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null || !npc.isSpawned() || npc.getEntity() == null) return;
        if (unlocked) {
            player.showEntity(plugin, npc.getEntity());
        } else {
            player.hideEntity(plugin, npc.getEntity());
        }
    }

    private void showSellBot(Player player) {
        refreshSellBotVisibility(player);
    }

    private void showCosmeticsNpc(Player player) {
        refreshCosmeticsNpcVisibility(player);
    }

    private void refreshNpcVisibilityForOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshSellBotVisibility(player);
            refreshCosmeticsNpcVisibility(player);
        }
    }

    private void startCompassTask() {
        compassTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, UUID> entry : activeGoats.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                Entity goat = Bukkit.getEntity(entry.getValue());
                if (player == null || goat == null || !goat.isValid() || goat.isDead()) continue;
                player.setCompassTarget(goat.getLocation());
            }
        }, 10L, plugin.getConfig().getLong("quest-master.quest-1.compass-update-ticks", 10L));
    }

    public void shutdown() {
        if (compassTask != null) compassTask.cancel();
        activeGoats.clear();
    }

    private String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
