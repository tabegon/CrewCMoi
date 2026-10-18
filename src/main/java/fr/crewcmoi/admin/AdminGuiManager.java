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

import java.util.List;

public final class AdminGuiManager {
    private final Main plugin;
    private final BountyReviewGuiManager bountyReviewGuiManager;
    private final ClaimAdminGuiManager claimAdminGuiManager;
    private final HomeAdminGuiManager homeAdminGuiManager;
    private final PlayerAdminGuiManager playerAdminGuiManager;

    public AdminGuiManager(Main plugin, BountyReviewGuiManager bountyReviewGuiManager,
                           ClaimAdminGuiManager claimAdminGuiManager, HomeAdminGuiManager homeAdminGuiManager,
                           PlayerAdminGuiManager playerAdminGuiManager) {
        this.plugin = plugin;
        this.bountyReviewGuiManager = bountyReviewGuiManager;
        this.claimAdminGuiManager = claimAdminGuiManager;
        this.homeAdminGuiManager = homeAdminGuiManager;
        this.playerAdminGuiManager = playerAdminGuiManager;
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
        inventory.setItem(31, item(Material.COMMAND_BLOCK, "&d&lOutils staff", List.of("&7Commandes utiles :", "&f/reset quest <joueur>", "&f/admin zone pos1|pos2|add|remove|list", "&f/duelarena 1|2 save|reset", "&f/staff et /vanish", "", "&eCliquer pour afficher l'aide")));
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
            case 31 -> {
                player.sendMessage(color("&8&m---------------- &d&lOutils staff &8&m----------------"));
                player.sendMessage(color("&e/reset quest <joueur> &7- Réinitialiser les quêtes"));
                player.sendMessage(color("&e/admin zone pos1|pos2|add <nom>|remove <nom>|list &7- Zones interdites aux claims"));
                player.sendMessage(color("&e/duelarena 1|2 save|reset &7- Sauvegarder ou restaurer une arène"));
                player.sendMessage(color("&e/staff &7et &e/vanish &7- Outils de modération"));
            }
            default -> { }
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) { meta.setDisplayName(color(name)); meta.setLore(lore.stream().map(this::color).toList()); stack.setItemMeta(meta); }
        return stack;
    }

    private String color(String value) { return ChatColor.translateAlternateColorCodes('&', value); }
}
