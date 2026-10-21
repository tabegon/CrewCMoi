package fr.crewcmoi.economie.managers;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
    private final NamespacedKey questOrderMigrationKey;
    private final Map<UUID, UUID> activeGoats = new HashMap<>();
    private final Map<UUID, Location> questTargets = new HashMap<>();
    private BukkitTask compassTask;

    public QuestManager(Main plugin) {
        this.plugin = plugin;
        this.completedKey = new NamespacedKey(plugin, "quest_1_completed");
        this.goatKey = new NamespacedKey(plugin, "quest_1_electric_goat");
        this.goatOwnerKey = new NamespacedKey(plugin, "quest_1_owner");
        this.quest2CompletedKey = new NamespacedKey(plugin, "quest_2_completed");
        this.questOrderMigrationKey = new NamespacedKey(plugin, "quest_order_swapped_v1");
        startCompassTask();
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshNpcVisibilityForOnlinePlayers, 20L);
    }


    public void removeQuestCompass(Player player) {
        if (player == null) return;
        NamespacedKey key = new NamespacedKey(plugin, "quest_1_compass");
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) continue;
            if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                player.getInventory().setItem(slot, null);
            }
        }
        player.updateInventory();
    }

    public boolean isCompleted(Player player) {
        migrateQuestOrder(player);
        return player.getPersistentDataContainer().getOrDefault(
                completedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
    }

    public boolean isQuest2Completed(Player player) {
        migrateQuestOrder(player);
        return player.getPersistentDataContainer().getOrDefault(
                quest2CompletedKey, PersistentDataType.BYTE, (byte) 0) == (byte) 1;
    }

    private void migrateQuestOrder(Player player) {
        var data = player.getPersistentDataContainer();
        if (data.has(questOrderMigrationKey, PersistentDataType.BYTE)) return;
        byte oldQuest1 = data.getOrDefault(completedKey, PersistentDataType.BYTE, (byte) 0);
        byte oldQuest2 = data.getOrDefault(quest2CompletedKey, PersistentDataType.BYTE, (byte) 0);
        data.set(completedKey, PersistentDataType.BYTE, oldQuest2);
        data.set(quest2CompletedKey, PersistentDataType.BYTE, oldQuest1);
        data.set(questOrderMigrationKey, PersistentDataType.BYTE, (byte) 1);
    }

    public boolean resetQuest(Player player, int questNumber) {
        if (player == null || (questNumber != 1 && questNumber != 2)) return false;
        if (questNumber == 1) {
            player.getPersistentDataContainer().remove(completedKey);
            refreshSellBotVisibility(player);
            refreshCosmeticsNpcVisibility(player);
        } else {
            player.getPersistentDataContainer().remove(quest2CompletedKey);
            UUID goatId = activeGoats.remove(player.getUniqueId());
            if (goatId != null) {
                Entity goat = Bukkit.getEntity(goatId);
                if (goat != null && goat.isValid()) goat.remove();
            }
            questTargets.remove(player.getUniqueId());
            removeQuestCompass(player);
        }
        refreshQuestLockedEntitiesVisibility(player);
        player.sendMessage(color("&eLa quête " + questNumber + " a été réinitialisée par un administrateur."));
        return true;
    }

    public void resetAllQuests(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        UUID goatId = activeGoats.remove(playerId);
        if (goatId != null) {
            Entity goat = Bukkit.getEntity(goatId);
            if (goat != null && goat.isValid()) goat.remove();
        }
        questTargets.remove(playerId);
        player.getPersistentDataContainer().remove(completedKey);
        player.getPersistentDataContainer().remove(quest2CompletedKey);
        NamespacedKey compassKey = new NamespacedKey(plugin, "quest_1_compass");
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) continue;
            if (item.getItemMeta().getPersistentDataContainer().has(compassKey, PersistentDataType.BYTE)) {
                player.getInventory().setItem(slot, null);
            }
        }
        refreshSellBotVisibility(player);
        refreshCosmeticsNpcVisibility(player);
        refreshQuestLockedEntitiesVisibility(player);
        player.sendMessage(color("&eToutes les quêtes ont été réinitialisées par un administrateur."));
    }

    /** Lance la quête de l'énigme, désormais première dans l'ordre des quêtes. */
    public void startQuest2(Player player) {
        if (isCompleted(player)) {
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
        player.sendMessage(color("ᴛʀᴏᴜᴠᴇ ᴍᴏɪ ᴅᴜ ᴘᴀᴘɪᴇʀ."));
    }

    /** Valide la quête de l'énigme, qui débloque le SellBot. */
    public boolean turnInQuest2Item(Player player) {
        if (isCompleted(player)) return false;
        if (!isQuest2Item(player.getInventory().getItemInMainHand())) return false;

        ItemStack hand = player.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        }

        player.getPersistentDataContainer().set(completedKey, PersistentDataType.BYTE, (byte) 1);
        player.sendMessage(color("ᴍᴇʀᴄɪ ᴊᴇ ꜱᴀɪꜱ ᴄᴇ ǫᴜ'ɪʟ ᴍᴇ ᴍᴀɴǫᴜᴇ, ᴛᴜ ᴘᴇᴜx ᴀʟʟᴇʀ ᴠᴇɴᴅʀᴇ ᴛᴇꜱ ᴏʙᴊᴇᴛꜱ ᴀᴜ ꜱᴇʟʟ ʙᴏᴛ."));
        showSellBot(player);
        refreshQuestLockedEntitiesVisibility(player);
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

        boolean grantWardrobeAccess = plugin.getConfig().getBoolean(
                "quest-master.quest-2.wardrobe-access.grant", true);
        if (grantWardrobeAccess) {
            String template = plugin.getConfig().getString(
                    "quest-master.quest-2.wardrobe-access.permission-command",
                    "lp user %player% permission set cosmeticscore.user.wardrobe.enterarea true");
            if (template != null && !template.trim().isEmpty()) {
                String resolved = template
                        .replace("%player%", player.getName())
                        .replace("%uuid%", player.getUniqueId().toString());
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
        if (!isCompleted(player)) {
            player.sendMessage(color(plugin.getConfig().getString("quest-master.quest-1.messages.locked", "&cTermine d'abord l'énigme.")));
            return;
        }
        if (isQuest2Completed(player)) {
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
            Location target = mountain.clone();
            Location start = findSafeSearchLocation(target);
            player.teleport(start);
            giveQuestCompass(player, target);
            trackGoat(player, target);
            return;
        }

        player.closeInventory();
        Location start = findSafeSearchLocation(mountain);
        player.teleport(start);
        questTargets.put(player.getUniqueId(), mountain.clone());
        giveQuestCompass(player, mountain);
        player.setCompassTarget(mountain);
        spawnGoat(player, mountain);
    }

    private void spawnGoat(Player player, Location location) {
        String mythicMob = plugin.getConfig().getString("quest-master.quest-1.mythic-mob", "Electric_Goat").trim();
        if (mythicMob.isEmpty()) {
            player.sendMessage(color("&cLe MythicMob de la quête n'est pas configuré."));
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            player.sendMessage(color("&cMythicMobs n'est pas disponible : impossible de faire apparaître l'Electric Goat."));
            plugin.getLogger().warning("Quête Electric Goat : MythicMobs n'est pas activé.");
            return;
        }

        Location spawnLocation = location.clone();
        spawnLocation.getChunk().load();
        if (!spawnLocation.getBlock().isPassable()
                || !spawnLocation.clone().add(0, 1, 0).getBlock().isPassable()) {
            int y = spawnLocation.getWorld().getHighestBlockYAt(
                    spawnLocation.getBlockX(), spawnLocation.getBlockZ()) + 1;
            spawnLocation.setY(y);
            while (y < spawnLocation.getWorld().getMaxHeight()
                    && (!spawnLocation.getBlock().isPassable()
                    || !spawnLocation.clone().add(0, 1, 0).getBlock().isPassable())) {
                y++;
                spawnLocation.setY(y);
            }
        }

        Set<UUID> before = new HashSet<>();
        for (Entity entity : spawnLocation.getWorld().getNearbyEntities(spawnLocation, 32, 32, 32)) {
            before.add(entity.getUniqueId());
        }

        String world = spawnLocation.getWorld().getName();
        String coordinates = world + "," + spawnLocation.getX() + "," + spawnLocation.getY() + ","
                + spawnLocation.getZ();
        String command = "mm mobs spawn -s " + mythicMob + ":1 1 " + coordinates;

        plugin.getLogger().info("Quête Electric Goat : exécution de '" + command + "' à "
                + spawnLocation.getWorld().getName() + " " + spawnLocation.getBlockX() + ","
                + spawnLocation.getBlockY() + "," + spawnLocation.getBlockZ());
        if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
            player.sendMessage(color("&cImpossible de lancer le spawn de l'Electric Goat. Consulte la console."));
            plugin.getLogger().warning("La commande de spawn MythicMobs a été refusée : " + command);
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> locateSpawnedGoat(player, spawnLocation, before, mythicMob, coordinates, 0), 3L);
    }

    private void locateSpawnedGoat(Player player, Location location, Set<UUID> before,
                                   String mythicMob, String coordinates, int attempt) {
        if (!player.isOnline() || location.getWorld() == null) return;

        Entity goat = findNewEntity(location, before);
        if (goat == null) {
            goat = findNearbyGoat(location);
        }
        if (goat != null) {
            registerQuestGoat(player, goat);
            plugin.getLogger().info("Electric Goat identifié : entity=" + goat.getType()
                    + ", uuid=" + goat.getUniqueId() + ", position="
                    + goat.getWorld().getName() + "," + goat.getLocation().getX() + ","
                    + goat.getLocation().getY() + "," + goat.getLocation().getZ());
            return;
        }

        if (attempt < 10) {
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> locateSpawnedGoat(player, location, before, mythicMob, coordinates, attempt + 1), 10L);
            return;
        }

        String nearby = location.getWorld().getNearbyEntities(location, 32, 32, 32).stream()
                .map(entity -> entity.getType() + "@" + entity.getLocation().getBlockX() + ","
                        + entity.getLocation().getBlockY() + "," + entity.getLocation().getBlockZ())
                .limit(20)
                .collect(java.util.stream.Collectors.joining("; "));
        player.sendMessage(color("&cMythicMobs annonce le spawn, mais le mob n'a pas pu être identifié. Consulte la console."));
        plugin.getLogger().warning("Electric Goat non détecté près de " + coordinates
                + ". Entités proches : " + (nearby.isEmpty() ? "aucune" : nearby));
    }

    private Entity findNearbyGoat(Location location) {
        Entity closest = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : location.getWorld().getNearbyEntities(location, 16, 16, 16)) {
            if (!entity.isValid() || entity.isDead() || entity instanceof Player) continue;
            if (!entity.getType().name().equals("GOAT")) continue;
            double distance = entity.getLocation().distanceSquared(location);
            if (distance < best) {
                best = distance;
                closest = entity;
            }
        }
        return closest;
    }

    private void registerQuestGoat(Player player, Entity goat) {
        goat.getPersistentDataContainer().set(goatKey, PersistentDataType.BYTE, (byte) 1);
        goat.getPersistentDataContainer().set(goatOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        activeGoats.put(player.getUniqueId(), goat.getUniqueId());
        trackGoat(player, goat);
    }

    private void giveQuestCompass(Player player, Location target) {
        if (target == null || target.getWorld() == null) return;

        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&e&lBoussole de l'Electric Goat"));
            meta.setLore(java.util.List.of(color("&7Elle pointe vers le point de recherche.")));
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "quest_1_compass"),
                    PersistentDataType.BYTE,
                    (byte) 1
            );
            compass.setItemMeta(meta);
        }

        NamespacedKey key = new NamespacedKey(plugin, "quest_1_compass");
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (current == null || current.getType() != Material.COMPASS || current.getItemMeta() == null) continue;
            Byte value = current.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
            if (value != null && value == (byte) 1) {
                player.getInventory().setItem(slot, compass);
                player.setCompassTarget(target);
                return;
            }
        }

        java.util.HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(compass);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
        player.setCompassTarget(target);
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
        for (Entity entity : location.getWorld().getNearbyEntities(location, 32, 32, 32)) {
            if (!entity.isValid() || entity.isDead() || entity instanceof Player) continue;
            if (before.contains(entity.getUniqueId())) continue;
            double distance = entity.getLocation().distanceSquared(location);
            if (distance < best) {
                best = distance;
                closest = entity;
            }
        }
        return closest;
    }

    private void trackGoat(Player player, Entity goat) {
        trackGoat(player, goat.getLocation());
    }

    private void trackGoat(Player player, Location target) {
        questTargets.put(player.getUniqueId(), target.clone());
        giveQuestCompass(player, target);
        player.setCompassTarget(target);
        player.sendMessage(color("ᴛᴜᴇ ᴄᴇᴛᴛᴇ ᴇʟᴇᴄᴛʀɪᴄ ɢᴏᴀᴛ ᴘᴏᴜʀ ᴍᴏɪ ᴇᴛ ᴅᴏɴɴᴇ ᴍᴏɪ ꜱᴀ ᴄᴏʀɴᴇ."));
    }

    private Location findSafeSearchLocation(Location target) {
        if (target == null || target.getWorld() == null) return target;

        int minRadius = Math.max(1, plugin.getConfig().getInt("quest-master.quest-1.search-radius-min", 180));
        int maxRadius = Math.max(minRadius, plugin.getConfig().getInt("quest-master.quest-1.search-radius-max", 220));
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < 40; attempt++) {
            double radius = random.nextDouble(minRadius, maxRadius + 1.0);
            double angle = random.nextDouble(0.0, Math.PI * 2.0);
            int x = (int) Math.round(target.getX() + Math.cos(angle) * radius);
            int z = (int) Math.round(target.getZ() + Math.sin(angle) * radius);
            int y = target.getWorld().getHighestBlockYAt(x, z) + 1;
            Location candidate = new Location(target.getWorld(), x + 0.5, y, z + 0.5, target.getYaw(), target.getPitch());

            if (candidate.getBlock().isPassable() && candidate.clone().add(0, 1, 0).getBlock().isPassable()
                    && candidate.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                return candidate;
            }
        }

        return target.clone().add(0, 1, 0);
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
        questTargets.remove(owner);
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
        questPlayer.sendMessage(color("&aTu as récupéré server:electric_ghoast_item ! &7Retourne voir le Quest Master."));
    }

    private ItemStack createHorn() {
        String itemsAdderId = plugin.getConfig().getString(
                "quest-master.quest-1.horn.itemsadder-id", "server:electric_ghoast_item").trim();
        if (itemsAdderId.isEmpty()) itemsAdderId = "server:electric_ghoast_item";
        CustomStack stack = CustomStack.getInstance(itemsAdderId);
        return stack == null ? null : stack.getItemStack().clone();
    }

    /** Valide la quête lorsque le joueur présente l'objet configuré au Quest Master. */
    public boolean turnInHorn(Player player) {
        if (!isCompleted(player) || isQuest2Completed(player) || !isHorn(player.getInventory().getItemInMainHand())) return false;

        ItemStack reward = createBrokenHornReward();
        if (reward == null) {
            player.sendMessage(color("&cImpossible de charger server:electric_goat_horn_broken depuis ItemsAdder. Rien n'a été consommé."));
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(reward);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.getPersistentDataContainer().set(quest2CompletedKey, PersistentDataType.BYTE, (byte) 1);
        player.sendMessage(color("ᴍᴇʀᴄɪ ᴀ̀ ᴛᴏɪ ᴊᴇ ᴛᴇ ᴅᴏɴɴᴇ ʟᴀ ᴍᴏɪᴛɪᴇ́, ᴀɪɴꜱɪ ǫᴜ'ᴜɴ ᴄᴏꜱᴍᴇᴛɪǫᴜᴇ ᴠᴀ ᴠᴏɪʀ ʟᴇ ɴᴘᴄ ᴀ̀ ᴄᴏᴛᴇ́ ᴅᴇ ʟᴀ ᴍᴀɪꜱᴏɴ ᴅᴜ ꜱᴇʟʟʙᴏᴛ."));
        showCosmeticsNpc(player);
        executeFirstCosmeticCommands(player);
        refreshQuestLockedEntitiesVisibility(player);
        return true;
    }

    private ItemStack createBrokenHornReward() {
        String itemsAdderId = plugin.getConfig().getString(
                "quest-master.quest-1.horn.turn-in-reward-itemsadder-id", "server:electric_goat_horn_broken").trim();
        if (itemsAdderId.isEmpty()) itemsAdderId = "server:electric_goat_horn_broken";
        CustomStack stack = CustomStack.getInstance(itemsAdderId);
        return stack == null ? null : stack.getItemStack().clone();
    }

    public boolean isHorn(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String itemsAdderId = plugin.getConfig().getString(
                "quest-master.quest-1.horn.itemsadder-id", "server:electric_ghoast_item").trim();
        if (itemsAdderId.isEmpty()) itemsAdderId = "server:electric_ghoast_item";
        CustomStack stack = CustomStack.byItemStack(item);
        return stack != null && itemsAdderId.equalsIgnoreCase(stack.getNamespacedID());
    }

    public void refreshSellBotVisibility(Player player) {
        boolean requireQuest = plugin.getConfig().getBoolean("sellbot.require-quest", true);
        boolean visible = !requireQuest || isCompleted(player);
        refreshNpcVisibility(player, "sell", visible);
        refreshSellBotModelVisibility(player, visible);
        refreshSellBotEntityVisibility(player, visible);
    }

    /** Cache aussi les ArmorStands utilisés comme parties du modèle ItemsAdder/Citizens du SellBot. */
    private void refreshSellBotModelVisibility(Player player, boolean visible) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) return;
        int npcId = plugin.getConfig().getInt("npcs.sell", 0);
        if (npcId <= 0) return;
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null || !npc.isSpawned() || npc.getEntity() == null) return;

        Entity anchor = npc.getEntity();
        double radius = Math.max(1.0, plugin.getConfig().getDouble("sellbot.model-armorstand-radius", 4.0));
        for (Entity nearby : anchor.getNearbyEntities(radius, radius, radius)) {
            if (!(nearby instanceof ArmorStand)) continue;
            if (visible) {
                player.showEntity(plugin, nearby);
            } else {
                player.hideEntity(plugin, nearby);
            }
        }
    }

    public void refreshCosmeticsNpcVisibility(Player player) {
        refreshNpcVisibility(player, "cosmetics", isQuest2Completed(player));
    }

    /** Ces trois entités sont visibles avant la quête 1, puis cachées après sa validation. */
    public void refreshQuestLockedEntitiesVisibility(Player player) {
        Set<UUID> lockedIds = getQuestLockedEntityUuids();
        Set<UUID> sellBotIds = getSellBotEntityUuids();
        if (lockedIds.isEmpty() && sellBotIds.isEmpty()) return;
        boolean questCompleted = isCompleted(player);
        for (UUID id : lockedIds) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) setEntityVisible(player, entity, !questCompleted);
        }
        for (UUID id : sellBotIds) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) setEntityVisible(player, entity, questCompleted);
        }
    }

    /** Actualise les deux groupes d'entités lorsqu'ils apparaissent ou sont chargés. */
    public void refreshQuestLockedEntityVisibility(Entity entity) {
        UUID id = entity.getUniqueId();
        boolean questEntity = getQuestLockedEntityUuids().contains(id);
        boolean sellBotEntity = getSellBotEntityUuids().contains(id);
        if (!questEntity && !sellBotEntity) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (questEntity) setEntityVisible(player, entity, !isCompleted(player));
            if (sellBotEntity) setEntityVisible(player, entity, isCompleted(player));
        }
    }

    private void refreshSellBotEntityVisibility(Player player, boolean visible) {
        for (UUID id : getSellBotEntityUuids()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) setEntityVisible(player, entity, visible);
        }
    }


    private void setEntityVisible(Player player, Entity entity, boolean visible) {
        if (visible) {
            player.showEntity(plugin, entity);
        } else {
            player.hideEntity(plugin, entity);
        }
    }

    private Set<UUID> getQuestLockedEntityUuids() {
        Set<UUID> ids = new HashSet<>();
        for (String configured : plugin.getConfig().getStringList("quest-visibility.locked-entity-uuids")) {
            if (configured == null || configured.isBlank()) continue;
            String[] parts = configured.split(",");
            if (parts.length != 4) {
                plugin.getLogger().warning("UUID d'entité invalide dans quest-visibility.locked-entity-uuids : " + configured);
                continue;
            }
            try {
                int a = Integer.parseInt(parts[0].trim());
                int b = Integer.parseInt(parts[1].trim());
                int c = Integer.parseInt(parts[2].trim());
                int d = Integer.parseInt(parts[3].trim());
                long most = ((long) a << 32) | (b & 0xffffffffL);
                long least = ((long) c << 32) | (d & 0xffffffffL);
                ids.add(new UUID(most, least));
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("UUID d'entité invalide dans quest-visibility.locked-entity-uuids : " + configured);
            }
        }
        return ids;
    }

    private Set<UUID> getSellBotEntityUuids() {
        Set<UUID> ids = new HashSet<>();
        for (String configured : plugin.getConfig().getStringList("sellbot.entity-uuids")) {
            if (configured == null || configured.isBlank()) continue;
            try {
                ids.add(UUID.fromString(configured.trim()));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("UUID SellBot invalide dans sellbot.entity-uuids : " + configured);
            }
        }
        return ids;
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
            refreshQuestLockedEntitiesVisibility(player);
        }
    }

    private void startCompassTask() {
        compassTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, Location> entry : questTargets.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null) continue;
                Location target = entry.getValue();
                if (target != null && target.getWorld() != null) {
                    player.setCompassTarget(target);
                }
            }
        }, 10L, plugin.getConfig().getLong("quest-master.quest-1.compass-update-ticks", 10L));
    }

    public void shutdown() {
        if (compassTask != null) compassTask.cancel();
        activeGoats.clear();
        questTargets.clear();
    }

    private String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
