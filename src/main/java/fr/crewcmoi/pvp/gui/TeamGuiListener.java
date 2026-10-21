package fr.crewcmoi.pvp.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.database.TeamData;
import fr.crewcmoi.pvp.managers.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import fr.crewcmoi.pvp.teamrequest.TeamRequestManager;

public class TeamGuiListener implements Listener {
    private final Main plugin;
    private final TeamManager teamManager;
    private final TeamGuiManager guiManager;
    private final TeamRequestGuiManager requestGuiManager;
    private final TeamRequestManager requestManager;
    private final TeamSettingsGuiManager settingsGuiManager;

    public TeamGuiListener(Main plugin, TeamManager teamManager, TeamGuiManager guiManager, TeamRequestGuiManager requestGuiManager, TeamRequestManager requestManager, TeamSettingsGuiManager settingsGuiManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.guiManager = guiManager;
        this.requestGuiManager = requestGuiManager;
        this.requestManager = requestManager;
        this.settingsGuiManager = settingsGuiManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof TeamSettingsHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;
            if (slot == TeamSettingsHolder.FRIENDLY_FIRE_SLOT) {
                settingsGuiManager.handleFriendlyFireClick(player, holder);
            } else if (slot == TeamSettingsHolder.BACK_SLOT) {
                settingsGuiManager.openTeamMenu(player, holder.getTeamId());
            } else if (slot == TeamSettingsHolder.CLOSE_SLOT) {
                player.closeInventory();
            }
            return;
        }
        if (event.getInventory().getHolder() instanceof TeamGuiHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            int slot = event.getRawSlot();
            if (slot == TeamGuiHolder.BACK_SLOT) {
                player.closeInventory();
                return;
            }
            if (slot == 22) {
                requestGuiManager.open(player);
                return;
            }
            if (TeamGuiHolder.isMemberSlot(slot)) {
                java.util.UUID member = holder.getMember(slot);
                if (member == null) return;
                teamManager.getTeamAsync(player.getUniqueId(), team -> {
                    if (team != null && team.isOwner(player.getUniqueId()) && team.isMember(member)) {
                        guiManager.openMember(player, member);
                    }
                });
            }
            return;
        }

        if (event.getInventory().getHolder() instanceof TeamRequestHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            requestGuiManager.handleClick(event, player, holder, event.getRawSlot());
            return;
        }

        if (event.getInventory().getHolder() instanceof MemberInfoHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            int slot = event.getRawSlot();

            if (slot == MemberInfoHolder.BACK_SLOT) {
                guiManager.open(player);
                return;
            }

            if (slot == MemberInfoHolder.EXCLUDE_SLOT) {
                teamManager.getTeamAsync(player.getUniqueId(), team -> {
                    if (team == null || !team.isOwner(player.getUniqueId())
                            || team.isOwner(holder.getMemberUuid())
                            || !team.isMember(holder.getMemberUuid())) {
                        return;
                    }
                    teamManager.removeMember(holder.getMemberUuid(), success -> {
                        if (success) guiManager.open(player);
                    });
                });
            }
        }
    }
    @EventHandler
    public void onRequestChat(AsyncPlayerChatEvent event) {
        if (!requestManager.hasPrompt(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = event.getMessage();
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> requestGuiManager.handleChat(player, message));
    }
}
