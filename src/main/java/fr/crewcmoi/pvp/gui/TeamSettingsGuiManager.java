package fr.crewcmoi.pvp.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.pvp.database.TeamData;
import fr.crewcmoi.pvp.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class TeamSettingsGuiManager {
    private final Main plugin;
    private final TeamManager teamManager;
    private final TeamGuiManager teamGuiManager;

    public TeamSettingsGuiManager(Main plugin, TeamManager teamManager, TeamGuiManager teamGuiManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.teamGuiManager = teamGuiManager;
    }

    public void open(Player player) {
        teamManager.getTeamAsync(player.getUniqueId(), team -> {
            if (team == null) {
                player.sendMessage(color("&cTu dois être dans une équipe pour modifier ses paramètres."));
                return;
            }
            open(player, team);
        });
    }

    public void open(Player player, TeamData team) {
        boolean owner = team.isOwner(player.getUniqueId());
        boolean friendlyFire = teamManager.isFriendlyFireEnabledForTeam(team.getId());
        TeamSettingsHolder holder = new TeamSettingsHolder(team.getId());
        Inventory inventory = Bukkit.createInventory(holder, TeamSettingsHolder.SIZE,
                color("&8&lParamètres : " + team.getName()));
        holder.setInventory(inventory);

        for (int slot = 0; slot < TeamSettingsHolder.SIZE; slot++) {
            inventory.setItem(slot, GuiItems.nothing(" "));
        }

        inventory.setItem(TeamSettingsHolder.FRIENDLY_FIRE_SLOT, settingItem(friendlyFire, owner));
        inventory.setItem(TeamSettingsHolder.BACK_SLOT, item(Material.ARROW, "&c&lRetour à l'équipe",
                List.of("&7Retourne à l'interface de l'équipe.")));
        inventory.setItem(TeamSettingsHolder.CLOSE_SLOT, item(Material.BARRIER, "&c&lFermer",
                List.of("&7Fermer ce menu.")));
        player.openInventory(inventory);
    }

    public void handleFriendlyFireClick(Player player, TeamSettingsHolder holder) {
        teamManager.getTeamAsync(player.getUniqueId(), team -> {
            if (team == null || team.getId() != holder.getTeamId()) {
                player.closeInventory();
                player.sendMessage(color("&cTu ne fais plus partie de cette équipe."));
                return;
            }
            if (!team.isOwner(player.getUniqueId())) {
                player.sendMessage(color("&cSeul le fondateur peut modifier les paramètres de l'équipe."));
                open(player, team);
                return;
            }
            boolean enabled = !teamManager.isFriendlyFireEnabledForTeam(team.getId());
            teamManager.setFriendlyFireEnabled(team.getId(), enabled);
            player.sendMessage(color(enabled
                    ? "&aLe PvP entre coéquipiers est maintenant activé."
                    : "&aLe PvP entre coéquipiers est maintenant désactivé."));
            open(player, team);
        });
    }

    public void openTeamMenu(Player player, int teamId) {
        teamManager.getTeamAsync(player.getUniqueId(), team -> {
            if (team != null && team.getId() == teamId) {
                teamGuiManager.open(player);
            } else {
                player.closeInventory();
            }
        });
    }

    private ItemStack settingItem(boolean enabled, boolean owner) {
        Material material = enabled ? Material.IRON_SWORD : Material.SHIELD;
        String state = enabled ? "&aACTIVÉ" : "&cDÉSACTIVÉ";
        List<String> lore = owner
                ? List.of("&7Autorise les membres à se", "&7faire des dégâts entre eux.", "", "&fÉtat : " + state, "", "&eClique pour changer l'état.")
                : List.of("&7Autorise les membres à se", "&7faire des dégâts entre eux.", "", "&fÉtat : " + state, "", "&cSeul le fondateur peut modifier ce réglage.");
        return item(material, "&6&lPvP entre coéquipiers", lore);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore.stream().map(this::color).toList());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
