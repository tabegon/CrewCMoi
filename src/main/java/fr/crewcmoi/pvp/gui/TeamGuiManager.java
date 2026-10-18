package fr.crewcmoi.pvp.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.database.PlayerData;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.other.utils.MoneyFormat;
import fr.crewcmoi.pvp.database.TeamData;
import fr.crewcmoi.pvp.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamGuiManager {
    private final Main plugin;
    private final TeamManager teamManager;

    public TeamGuiManager(Main plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public void open(Player player) {
        teamManager.getTeamAsync(player.getUniqueId(), team -> {
            if (team == null) {
                player.sendMessage(color(plugin.getMessages().getString("prefix", "") + plugin.getMessages().getString(
                        "teams.generic.not-in-teamxx", "&cVous n'êtes pas dans une équipe.")));
                return;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                double total = 0.0;
                for (UUID uuid : team.getMembers().keySet()) {
                    PlayerData data = plugin.getDatabaseManager().getPlayer(uuid);
                    if (data != null) total += data.getBalance();
                }
                double finalTotal = total;
                Bukkit.getScheduler().runTask(plugin, () -> openTeamInventory(player, team, finalTotal));
            });
        });
    }

    public void openTeamInventory(Player player, TeamData team, double total) {
        TeamGuiHolder holder = new TeamGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, TeamGuiHolder.SIZE,
                color("&8&lTeam : " + team.getName()));
        holder.setInventory(inv);

        for (int slot = 0; slot < TeamGuiHolder.SIZE; slot++) {
            inv.setItem(slot, GuiItems.nothing(" "));
        }
        inv.setItem(TeamGuiHolder.BACK_SLOT, GuiItems.cancelButton("&c&lɢᴏ ʙᴀᴄᴋ"));
        inv.setItem(TeamGuiHolder.MONEY_SLOT, money("&e&lᴀʀɢᴇɴᴛ ᴛᴏᴛᴀʟ", MoneyFormat.format(total)));
        inv.setItem(22, GuiItems.moneyButton("&6&lᴅᴇᴍᴀɴᴅᴇs ᴅ'éǫᴜɪᴘᴇ"));

        int index = 0;
        for (Map.Entry<UUID, String> entry : team.getMembers().entrySet()) {
            if (index >= TeamGuiHolder.memberSlots().length) break;
            int slot = TeamGuiHolder.memberSlots()[index++];
            inv.setItem(slot, playerHead(entry.getKey(), entry.getValue(), team.getRole(entry.getKey())));
            holder.mapMember(slot, entry.getKey());
        }

        player.openInventory(inv);
    }

    public void openMember(Player viewer, UUID memberUuid) {
        teamManager.getTeamAsync(viewer.getUniqueId(), team -> {
            if (team == null || !team.isMember(memberUuid)) return;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                PlayerData data = plugin.getDatabaseManager().getPlayer(memberUuid);
                double balance = data == null ? 0.0 : data.getBalance();
                String name = team.getMembers().getOrDefault(memberUuid, Bukkit.getOfflinePlayer(memberUuid).getName());
                boolean canExclude = team.isOwner(viewer.getUniqueId()) && !team.isOwner(memberUuid);
                double finalBalance = balance;
                String role = team.getRole(memberUuid);
                Bukkit.getScheduler().runTask(plugin, () -> openMemberInventory(viewer, memberUuid, name, finalBalance, team.getOwnerUuid(), canExclude, role));
            });
        });
    }

    private void openMemberInventory(Player viewer, UUID memberUuid, String name, double balance, UUID ownerUuid, boolean canExclude, String role) {
        MemberInfoHolder holder = new MemberInfoHolder(memberUuid, ownerUuid);
        Inventory inv = Bukkit.createInventory(holder, MemberInfoHolder.SIZE, color("&8&lInfo " + name));
        holder.setInventory(inv);
        for (int slot = 0; slot < MemberInfoHolder.SIZE; slot++) {
            inv.setItem(slot, GuiItems.nothing(" "));
        }

        inv.setItem(MemberInfoHolder.BACK_SLOT, GuiItems.cancelButton("&c&lɢᴏ ʙᴀᴄᴋ"));
        inv.setItem(MemberInfoHolder.PLAYER_HEAD_SLOT, playerHead(memberUuid, name, role));
        inv.setItem(MemberInfoHolder.MONEY_SLOT, money("&e&lᴀʀɢᴇɴᴛ", MoneyFormat.format(balance)));

        if (canExclude) {
            inv.setItem(MemberInfoHolder.EXCLUDE_SLOT, GuiItems.barrier("&c&lᴇxᴄʟᴜʀᴇʀ ᴄᴇ ᴊᴏᴜᴇᴜʀ"));
        }

        viewer.openInventory(inv);
    }

    private ItemStack playerHead(UUID uuid, String name, String role) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            String roleLabel = switch (role) {
                case "FOUNDER" -> "&6Fondateur";
                case "CO_FOUNDER" -> "&dCo-fondateur";
                case "OFFICER" -> "&bOfficier";
                default -> "&7Membre";
            };
            String nameColor = switch (role) {
                case "FOUNDER" -> "&6&l";
                case "CO_FOUNDER" -> "&d&l";
                case "OFFICER" -> "&b&l";
                default -> "&e&l";
            };
            meta.setDisplayName(color(nameColor + name));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Grade : " + roleLabel));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack money(String name, String amount) {
        ItemStack item = GuiItems.moneyButton(name + " : &a" + amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of(color("&7Montant : &a" + amount)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
