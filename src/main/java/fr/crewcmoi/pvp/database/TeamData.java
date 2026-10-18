package fr.crewcmoi.pvp.database;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TeamData {

    private final int id;
    private final String name;
    private final UUID ownerUuid;
    
    private final Map<UUID, String> members;
    private final Map<UUID, String> roles;

    public TeamData(int id, String name, UUID ownerUuid, Map<UUID, String> members, Map<UUID, String> roles) {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.members = new LinkedHashMap<>(members);
        this.roles = new LinkedHashMap<>(roles);
        this.roles.put(ownerUuid, "FOUNDER");
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

    public String getRole(UUID uuid) {
        if (isOwner(uuid)) return "FOUNDER";
        return roles.getOrDefault(uuid, "MEMBER");
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
