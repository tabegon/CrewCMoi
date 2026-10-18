package fr.crewcmoi.pvp.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TeamRequestHolder implements InventoryHolder {
    public static final int SIZE = 27;
    private final int teamId;
    private final boolean leaderboard;
    private final Map<Integer,Integer> requests = new LinkedHashMap<>();
    private Inventory inventory;
    public TeamRequestHolder(int teamId, boolean leaderboard) { this.teamId=teamId; this.leaderboard=leaderboard; }
    public int getTeamId() { return teamId; }
    public boolean isLeaderboard() { return leaderboard; }
    public void setInventory(Inventory inventory) { this.inventory=inventory; }
    @Override public Inventory getInventory() { return inventory; }
    public void mapRequest(int slot,int id) { requests.put(slot,id); }
    public Integer getRequest(int slot) { return requests.get(slot); }
}
