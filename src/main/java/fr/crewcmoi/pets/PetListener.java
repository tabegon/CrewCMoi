package fr.crewcmoi.pets;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class PetListener implements Listener {
    private final Main plugin;
    private final PetManager petManager;
    private final PetGuiManager guiManager;

    public PetListener(Main plugin, PetManager petManager, PetGuiManager guiManager) {
        this.plugin = plugin;
        this.petManager = petManager;
        this.guiManager = guiManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPetItemRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack.getType().isAir()) return;

        for (String petId : petManager.getConfiguredPets()) {
            if (!petManager.isPetItem(stack, petId)) continue;

            event.setCancelled(true);
            if (petManager.owns(player.getUniqueId(), petId)) {
                player.sendMessage("§cVous possédez déjà ce pet.");
                return;
            }

            petManager.grant(player.getUniqueId(), petId);
            if (stack.getAmount() > 1) stack.setAmount(stack.getAmount() - 1);
            else player.getInventory().setItemInMainHand(null);

            player.sendMessage("§aPet §e" + petId + " §adébloqué !");
            guiManager.open(player);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPetGuiClick(InventoryClickEvent event) {
        if (!PetGuiManager.TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        for (String petId : petManager.getConfiguredPets()) {
            if (!petManager.isPetItem(clicked, petId)) continue;
            if (!petManager.owns(player.getUniqueId(), petId)) return;

            petManager.toggle(player, petId);
            plugin.getServer().getScheduler().runTask(plugin, () -> guiManager.open(player));
            return;
        }
    }

    private boolean isOwnedPet(org.bukkit.entity.Entity entity) {
        return entity.getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "crew_pet_owner"),
                PersistentDataType.STRING
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPetDamage(EntityDamageEvent event) {
        if (isOwnedPet(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPetAttack(EntityDamageByEntityEvent event) {
        if (isOwnedPet(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPetTarget(EntityTargetLivingEntityEvent event) {
        if (isOwnedPet(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPetDeath(EntityDeathEvent event) {
        String owner = event.getEntity().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "crew_pet_owner"),
                PersistentDataType.STRING
        );
        if (owner == null) return;

        try {
            Player player = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(owner));
            if (player != null) {
                petManager.deactivate(player, true);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        petManager.handleQuit(event.getPlayer());
    }
}
