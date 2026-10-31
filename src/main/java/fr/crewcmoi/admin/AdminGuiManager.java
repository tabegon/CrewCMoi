package fr.crewcmoi.admin;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.gui.ClaimAdminGuiManager;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.pvp.gui.BountyReviewGuiManager;
import fr.crewcmoi.teleport.gui.HomeAdminGuiManager;
import fr.crewcmoi.teleport.gui.PlayerAdminGuiManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.UUID;

import java.util.List;

public final class AdminGuiManager {
    private final Main plugin;
    private final BountyReviewGuiManager bountyReviewGuiManager;
    private final ClaimAdminGuiManager claimAdminGuiManager;
    private final HomeAdminGuiManager homeAdminGuiManager;
    private final PlayerAdminGuiManager playerAdminGuiManager;
    private final OfflineInventoryStore offlineInventoryStore;

    public AdminGuiManager(Main plugin, BountyReviewGuiManager bountyReviewGuiManager,
                           ClaimAdminGuiManager claimAdminGuiManager, HomeAdminGuiManager homeAdminGuiManager,
                           PlayerAdminGuiManager playerAdminGuiManager) {
        this.plugin = plugin;
        this.bountyReviewGuiManager = bountyReviewGuiManager;
        this.claimAdminGuiManager = claimAdminGuiManager;
        this.homeAdminGuiManager = homeAdminGuiManager;
        this.playerAdminGuiManager = playerAdminGuiManager;
        this.offlineInventoryStore = new OfflineInventoryStore(plugin);
    }

    public OfflineInventoryStore getOfflineInventoryStore() {
        return offlineInventoryStore;
    }

    public void open(Player player) {
        AdminGuiHolder holder = new AdminGuiHolder();
        Inventory inventory = Bukkit.createInventory(holder, 45, color("&8&lAdministration du serveur"));
        holder.setInventory(inventory);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, GuiItems.nothing(" "));
        inventory.setItem(10, item(Material.WRITABLE_BOOK, "&c&lRévision des primes", List.of("&7Examiner les raisons en attente", "", "&eCliquer pour ouvrir")));
        inventory.setItem(12, item(Material.IRON_BLOCK, "&a&lGestion des claims", List.of("&7Consulter et administrer tous les claims", "", "&eCliquer pour ouvrir")));
        inventory.setItem(14, item(Material.ENDER_PEARL, "&b&lHomes des joueurs", List.of("&7Consulter les homes et s'y téléporter", "", "&eCliquer pour ouvrir")));
        inventory.setItem(16, item(Material.PLAYER_HEAD, "&e&lInformations des joueurs", List.of("&7Soldes, homes, claims, équipe et primes", "", "&eCliquer pour ouvrir")));
        inventory.setItem(20, item(Material.LEAD, "&b&lModération des pets", List.of("&7Retirer des pets obtenus par un joueur", "&f/admin pets <joueur> <pet|1|2|all>", "", "&eCliquer pour afficher l'aide")));
        inventory.setItem(22, item(Material.BOOK, "&d&lModération des quêtes", List.of("&7Réinitialiser une ou plusieurs quêtes", "&f/admin quest <joueur> <1|2|all>", "", "&eCliquer pour afficher l'aide")));
        inventory.setItem(24, GuiItems.chestButton("&b&lInventaires des joueurs"));
        inventory.getItem(24).setItemMeta(withLore(inventory.getItem(24), List.of("&7Modifier l'inventaire ou l'ender chest", "", "&eCliquer pour ouvrir")));
        inventory.setItem(31, item(Material.COMMAND_BLOCK, "&d&lOutils staff", List.of("&7Commandes utiles :", "&f/pvpgroup <joueur> <0|1|2>", "&f/admin zone pos1|pos2|add|remove|list", "&f/duelarena 1|2 save|reset", "&f/staff et /vanish", "", "&eCliquer pour afficher l'aide")));
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return;
        switch (event.getRawSlot()) {
            case 10 -> bountyReviewGuiManager.open(player, 0);
            case 12 -> claimAdminGuiManager.open(player);
            case 14 -> homeAdminGuiManager.open(player);
            case 16 -> playerAdminGuiManager.open(player);
            case 20 -> openPlayerSelection(player, AdminModerationHolder.Mode.PET_PLAYERS);
            case 22 -> openPlayerSelection(player, AdminModerationHolder.Mode.QUEST_PLAYERS);
            case 24 -> openPlayerSelection(player, AdminModerationHolder.Mode.INVENTORY_PLAYERS);
            case 31 -> {
                player.sendMessage(color("&8&m---------------- &d&lOutils staff &8&m----------------"));
                player.sendMessage(color("&e/reset quest <joueur> &7- Réinitialiser toutes les quêtes"));
                player.sendMessage(color("&e/pvpgroup <joueur> <0|1|2> &7- Définir le groupe de punition PvP"));
                player.sendMessage(color("&e/admin zone pos1|pos2|add <nom>|remove <nom>|list &7- Zones interdites aux claims"));
                player.sendMessage(color("&e/duelarena 1|2 save|reset &7- Sauvegarder ou restaurer une arène"));
                player.sendMessage(color("&e/staff &7et &e/vanish &7- Outils de modération"));
            }
            default -> { }
        }
    }

    private void openPlayerSelection(Player viewer, AdminModerationHolder.Mode mode) {
        openPlayerSelection(viewer, mode, 0);
    }

    private void openPlayerSelection(Player viewer, AdminModerationHolder.Mode mode, int requestedPage) {
        List<UUID> targets = (mode == AdminModerationHolder.Mode.INVENTORY_PLAYERS
                ? java.util.Arrays.stream(Bukkit.getOfflinePlayers()).map(org.bukkit.OfflinePlayer::getUniqueId)
                : Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId))
                .distinct()
                .sorted((a, b) -> {
                    String nameA = Bukkit.getOfflinePlayer(a).getName();
                    String nameB = Bukkit.getOfflinePlayer(b).getName();
                    return String.CASE_INSENSITIVE_ORDER.compare(
                            nameA == null ? a.toString() : nameA,
                            nameB == null ? b.toString() : nameB);
                }).toList();
        int maxPage = Math.max(0, (targets.size() - 1) / 45);
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        AdminModerationHolder holder = new AdminModerationHolder(mode, null, targets, page);
        String type = mode == AdminModerationHolder.Mode.PET_PLAYERS ? "pets"
                : mode == AdminModerationHolder.Mode.QUEST_PLAYERS ? "quêtes" : "inventaire";
        Inventory inventory = Bukkit.createInventory(holder, 54,
                color("&8Choisir un joueur — " + type + " &7(" + (page + 1) + "/" + (maxPage + 1) + ")"));
        holder.setInventory(inventory);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, GuiItems.nothing(" "));
        int start = page * 45;
        int end = Math.min(start + 45, targets.size());
        for (int index = start; index < end; index++) {
            UUID uuid = targets.get(index);
            org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(uuid);
            String targetName = offlineTarget.getName() == null ? uuid.toString() : offlineTarget.getName();
            boolean isOnline = offlineTarget.isOnline();
            List<String> lore = new ArrayList<>();
            lore.add("&7Cliquer pour gérer " + (mode == AdminModerationHolder.Mode.PET_PLAYERS
                    ? "ses pets" : mode == AdminModerationHolder.Mode.QUEST_PLAYERS
                    ? "ses quêtes" : "son inventaire"));
            if (!isOnline && mode == AdminModerationHolder.Mode.INVENTORY_PLAYERS) {
                lore.add("&8Joueur hors ligne");
                if (!offlineInventoryStore.hasSnapshot(uuid)) lore.add("&cAucun instantané disponible");
            }
            inventory.setItem(index - start, item(Material.PLAYER_HEAD, "&e" + targetName, lore));
        }
        if (page > 0) inventory.setItem(45, item(Material.ARROW, "&e« Page précédente", List.of()));
        inventory.setItem(49, item(Material.BARRIER, "&cRetour au menu admin", List.of()));
        if (end < targets.size()) inventory.setItem(53, item(Material.ARROW, "&ePage suivante »", List.of()));
        viewer.openInventory(inventory);
    }

    public boolean handleModerationClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminModerationHolder holder)) return false;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player viewer)) return true;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return true;

        if (holder.getMode() == AdminModerationHolder.Mode.PET_PLAYERS
                || holder.getMode() == AdminModerationHolder.Mode.QUEST_PLAYERS
                || holder.getMode() == AdminModerationHolder.Mode.INVENTORY_PLAYERS) {
            int start = holder.getPage() * 45;
            if (slot == 45 && holder.getPage() > 0) {
                openPlayerSelection(viewer, holder.getMode(), holder.getPage() - 1);
                return true;
            }
            if (slot == 53 && start + 45 < holder.getPlayers().size()) {
                openPlayerSelection(viewer, holder.getMode(), holder.getPage() + 1);
                return true;
            }
            if (slot == 49) {
                open(viewer);
                return true;
            }
            if (slot < 0 || slot >= 45 || start + slot >= holder.getPlayers().size()) return true;
            UUID targetId = holder.getPlayers().get(start + slot);
            Player target = Bukkit.getPlayer(targetId);
            if (holder.getMode() == AdminModerationHolder.Mode.INVENTORY_PLAYERS) {
                String targetName = Bukkit.getOfflinePlayer(targetId).getName();
                if (targetName == null) targetName = targetId.toString();
                if (target == null && !offlineInventoryStore.hasSnapshot(targetId)) {
                    viewer.sendMessage(color("&cAucun inventaire enregistré pour ce joueur hors ligne. Il devra se connecter une fois avec cette version du plugin."));
                    return true;
                }
                openInventoryActionMenu(viewer, targetId, targetName);
            } else {
                if (target == null) {
                    viewer.sendMessage(color("&cCe joueur n'est plus connecté."));
                    return true;
                }
                openActionMenu(viewer, target, holder.getMode() == AdminModerationHolder.Mode.PET_PLAYERS);
            }
            return true;
        }

        Player target = Bukkit.getPlayer(holder.getTarget());
        if (holder.getMode() == AdminModerationHolder.Mode.INVENTORY_ACTIONS) {
            String targetName = Bukkit.getOfflinePlayer(holder.getTarget()).getName();
            if (targetName == null) targetName = holder.getTarget().toString();
            if (slot == 40) {
                openPlayerSelection(viewer, AdminModerationHolder.Mode.INVENTORY_PLAYERS);
            } else if (slot == 20) {
                openPlayerInventoryEditor(viewer, holder.getTarget(), targetName, AdminPlayerInventoryHolder.Type.PLAYER_INVENTORY);
            } else if (slot == 24) {
                openPlayerInventoryEditor(viewer, holder.getTarget(), targetName, AdminPlayerInventoryHolder.Type.ENDER_CHEST);
            }
            return true;
        }
        if (target == null) {
            viewer.sendMessage(color("&cCe joueur n'est plus connecté."));
            viewer.closeInventory();
            return true;
        }

        if (holder.getMode() == AdminModerationHolder.Mode.PET_ACTIONS) {
            var pets = plugin.getPetManager();
            if (pets == null) return true;
            if (slot == 45) {
                int removed = pets.resetOwnedPets(target.getUniqueId(), 1);
                viewer.sendMessage(color("&a" + removed + " pet retiré à " + target.getName() + "."));
                openActionMenu(viewer, target, true);
            } else if (slot == 46) {
                int removed = pets.resetOwnedPets(target.getUniqueId(), 2);
                viewer.sendMessage(color("&a" + removed + " pet(s) retiré(s) à " + target.getName() + "."));
                openActionMenu(viewer, target, true);
            } else if (slot == 48) {
                int removed = pets.resetOwnedPets(target.getUniqueId(), Integer.MAX_VALUE);
                viewer.sendMessage(color("&a" + removed + " pet(s) retiré(s) à " + target.getName() + "."));
                openActionMenu(viewer, target, true);
            } else {
                List<String> owned = new ArrayList<>(pets.getOwnedPets(target.getUniqueId()));
                int index = slot;
                if (index < owned.size()) {
                    String petId = owned.get(index);
                    boolean removed = pets.removeOwnedPet(target.getUniqueId(), petId);
                    if (removed) {
                        viewer.sendMessage(color("&aPet &e" + petId + "&a retiré à " + target.getName() + "."));
                    } else {
                        long remaining = pets.getRemovalLockRemainingSeconds(target.getUniqueId(), petId);
                        viewer.sendMessage(color(remaining > 0
                                ? "&cCe pet est verrouillé pendant encore " + remaining + " seconde(s)."
                                : "&cCe pet n'a pas pu être retiré."));
                    }
                    openActionMenu(viewer, target, true);
                }
            }
            return true;
        }

        if (holder.getMode() == AdminModerationHolder.Mode.QUEST_ACTIONS) {
            if (slot == 20) plugin.getQuestManager().resetQuest(target, 1);
            else if (slot == 22) plugin.getQuestManager().resetQuest(target, 2);
            else if (slot == 24) plugin.getQuestManager().resetAllQuests(target);
            else return true;
            viewer.sendMessage(color("&aProgression des quêtes mise à jour pour " + target.getName() + "."));
            openActionMenu(viewer, target, false);
        }
        return true;
    }

    private void openInventoryActionMenu(Player viewer, UUID targetId, String targetName) {
        AdminModerationHolder holder = new AdminModerationHolder(AdminModerationHolder.Mode.INVENTORY_ACTIONS, targetId, List.of());
        Inventory inventory = Bukkit.createInventory(holder, 45, color("&8Inventaire de " + targetName));
        holder.setInventory(inventory);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, GuiItems.nothing(" "));
        inventory.setItem(20, item(Material.CHEST, "&a&lInventaire du joueur", List.of("&7Modifier les objets, l'armure et la main secondaire", "", "&eCliquer pour ouvrir")));
        inventory.setItem(24, item(Material.ENDER_CHEST, "&5&lEnder chest", List.of("&7Modifier le contenu de l'ender chest", "", "&eCliquer pour ouvrir")));
        inventory.setItem(40, item(Material.ARROW, "&eRetour à la liste", List.of("&7Revenir au choix du joueur")));
        viewer.openInventory(inventory);
    }

    private void openPlayerInventoryEditor(Player viewer, UUID targetId, String targetName, AdminPlayerInventoryHolder.Type type) {
        Player onlineTarget = Bukkit.getPlayer(targetId);
        boolean offline = onlineTarget == null;
        if (offline && !offlineInventoryStore.hasSnapshot(targetId)) {
            viewer.sendMessage(color("&cAucun instantané d'inventaire disponible pour ce joueur hors ligne."));
            return;
        }
        AdminPlayerInventoryHolder holder = new AdminPlayerInventoryHolder(targetId, type, targetName, offline);
        String title = type == AdminPlayerInventoryHolder.Type.PLAYER_INVENTORY ? "&8Inventaire : " : "&8Ender chest : ";
        Inventory inventory = Bukkit.createInventory(holder, 54, color(title + targetName));
        holder.setInventory(inventory);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, GuiItems.nothing(" "));
        if (type == AdminPlayerInventoryHolder.Type.PLAYER_INVENTORY) {
            ItemStack[] contents = offline ? loadOfflineStorage(targetId) : onlineTarget.getInventory().getStorageContents();
            for (int i = 0; i < Math.min(36, contents.length); i++) inventory.setItem(i, contents[i]);
            ItemStack[] armor = offline ? loadOfflineArmor(targetId) : onlineTarget.getInventory().getArmorContents();
            for (int i = 0; i < Math.min(4, armor.length); i++) inventory.setItem(45 + i, armor[i]);
            inventory.setItem(50, offline ? offlineInventoryStore.getItem(targetId, "inventory.offhand") : onlineTarget.getInventory().getItemInOffHand());
            inventory.setItem(49, GuiItems.nothing(offline
                    ? "&8Hors ligne : les changements seront appliqués à sa prochaine connexion"
                    : "&8Armure au-dessus / main secondaire à droite"));
            inventory.setItem(51, GuiItems.nothing(" "));
            inventory.setItem(52, GuiItems.nothing(" "));
        } else {
            ItemStack[] contents = offline ? loadOfflineEnder(targetId) : onlineTarget.getEnderChest().getContents();
            for (int i = 0; i < Math.min(27, contents.length); i++) inventory.setItem(i, contents[i]);
        }
        inventory.setItem(53, GuiItems.barrier("&cEnregistrer et revenir"));
        viewer.openInventory(inventory);
    }

    private ItemStack[] loadOfflineStorage(UUID uuid) {
        ItemStack[] items = new ItemStack[36];
        for (int i = 0; i < items.length; i++) items[i] = offlineInventoryStore.getItem(uuid, "inventory.storage." + i);
        return items;
    }

    private ItemStack[] loadOfflineArmor(UUID uuid) {
        ItemStack[] items = new ItemStack[4];
        for (int i = 0; i < items.length; i++) items[i] = offlineInventoryStore.getItem(uuid, "inventory.armor." + i);
        return items;
    }

    private ItemStack[] loadOfflineEnder(UUID uuid) {
        ItemStack[] items = new ItemStack[27];
        for (int i = 0; i < items.length; i++) items[i] = offlineInventoryStore.getItem(uuid, "ender." + i);
        return items;
    }

    public boolean handleInventoryEditorClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminPlayerInventoryHolder)) return false;
        int slot = event.getRawSlot();
        if (slot == 53 && event.getWhoClicked() instanceof Player player) {
            event.setCancelled(true);
            AdminPlayerInventoryHolder holder = (AdminPlayerInventoryHolder) event.getInventory().getHolder();
            saveInventoryEditor(event.getInventory(), holder);
            openInventoryActionMenu(player, holder.getTarget(), holder.getTargetName());
            return true;
        }
        if (slot < 0 || slot >= event.getInventory().getSize() || event.getClick().isShiftClick() || event.getClick().isKeyboardClick()) {
            event.setCancelled(true);
            return true;
        }
        AdminPlayerInventoryHolder holder = (AdminPlayerInventoryHolder) event.getInventory().getHolder();
        boolean allowed = holder.getType() == AdminPlayerInventoryHolder.Type.ENDER_CHEST ? slot < 27 : (slot < 36 || (slot >= 45 && slot <= 48) || slot == 50);
        if (!allowed) event.setCancelled(true);
        return true;
    }

    public void handleInventoryEditorClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AdminPlayerInventoryHolder holder) saveInventoryEditor(event.getInventory(), holder);
    }

    public boolean handleInventoryEditorDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminPlayerInventoryHolder holder)) return false;
        for (int rawSlot : event.getRawSlots()) {
            boolean allowed = holder.getType() == AdminPlayerInventoryHolder.Type.ENDER_CHEST ? rawSlot < 27 : (rawSlot < 36 || (rawSlot >= 45 && rawSlot <= 48) || rawSlot == 50);
            if (rawSlot >= event.getInventory().getSize() || !allowed) { event.setCancelled(true); break; }
        }
        return true;
    }

    private void saveInventoryEditor(Inventory inventory, AdminPlayerInventoryHolder holder) {
        Player target = Bukkit.getPlayer(holder.getTarget());
        if (target == null) {
            offlineInventoryStore.saveEditor(holder.getTarget(), holder.getType(), inventory);
            return;
        }
        if (holder.getType() == AdminPlayerInventoryHolder.Type.ENDER_CHEST) {
            ItemStack[] contents = new ItemStack[27];
            for (int i = 0; i < 27; i++) contents[i] = inventory.getItem(i);
            target.getEnderChest().setContents(contents);
        } else {
            ItemStack[] contents = new ItemStack[36];
            for (int i = 0; i < 36; i++) contents[i] = inventory.getItem(i);
            target.getInventory().setStorageContents(contents);
            ItemStack[] armor = new ItemStack[4];
            for (int i = 0; i < 4; i++) armor[i] = inventory.getItem(45 + i);
            target.getInventory().setArmorContents(armor);
            target.getInventory().setItemInOffHand(inventory.getItem(50));
            target.updateInventory();
            offlineInventoryStore.saveSnapshot(target);
        }
    }

    private void openActionMenu(Player viewer, Player target, boolean petsMode) {
        AdminModerationHolder.Mode mode = petsMode ? AdminModerationHolder.Mode.PET_ACTIONS : AdminModerationHolder.Mode.QUEST_ACTIONS;
        AdminModerationHolder holder = new AdminModerationHolder(mode, target.getUniqueId(), List.of());
        Inventory inventory = Bukkit.createInventory(holder, 54,
                color(petsMode ? "&8Pets de " + target.getName() : "&8Quêtes de " + target.getName()));
        holder.setInventory(inventory);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, GuiItems.nothing(" "));
        if (petsMode) {
            var pets = plugin.getPetManager();
            if (pets != null) {
                List<String> owned = new ArrayList<>(pets.getOwnedPets(target.getUniqueId()));
                for (int i = 0; i < Math.min(owned.size(), 45); i++) {
                    String petId = owned.get(i);
                    long remaining = pets.getRemovalLockRemainingSeconds(target.getUniqueId(), petId);
                    inventory.setItem(i, item(Material.LEAD, "&b" + petId, remaining > 0
                            ? List.of("&cVerrouillé encore " + remaining + " s")
                            : List.of("&7Retirer ce pet", "&eCliquer pour confirmer")));
                }
            }
            inventory.setItem(45, item(Material.REDSTONE, "&cRetirer 1 pet", List.of("&7Retire un pet possédé")));
            inventory.setItem(46, item(Material.REDSTONE_BLOCK, "&cRetirer 2 pets", List.of("&7Retire jusqu'à deux pets")));
            inventory.setItem(48, item(Material.BARRIER, "&4Réinitialiser tous les pets", List.of("&7Retire tous les pets débloqués")));
        } else {
            inventory.setItem(20, item(Material.BOOK, "&eRéinitialiser la quête 1", List.of("&7Effacer la progression de la quête 1")));
            inventory.setItem(22, item(Material.WRITABLE_BOOK, "&eRéinitialiser la quête 2", List.of("&7Effacer la progression de la quête 2")));
            inventory.setItem(24, item(Material.BARRIER, "&cRéinitialiser toutes les quêtes", List.of("&7Effacer la progression des deux quêtes")));
        }
        viewer.openInventory(inventory);
    }

    private ItemMeta withLore(ItemStack stack, List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) meta.setLore(lore.stream().map(this::color).toList());
        return meta;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) { meta.setDisplayName(color(name)); meta.setLore(lore.stream().map(this::color).toList()); stack.setItemMeta(meta); }
        return stack;
    }

    private String color(String value) { return ChatColor.translateAlternateColorCodes('&', value); }
}
