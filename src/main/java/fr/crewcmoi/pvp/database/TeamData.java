package fr.crewcmoi.pvp.database;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TeamData {

    private final int id;
    private final String name;
    private final UUID ownerUuid;
    
    private final Map<UUID, String> members;

    public TeamData(int id, String name, UUID ownerUuid, Map<UUID, String> members) {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.members = new LinkedHashMap<>(members);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public Map<UUID, String> getMembers() {
        return members;
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid.equals(uuid);
    }

    public int size() {
        return members.size();
    }
}
