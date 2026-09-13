package fr.crewcmoi.teleport.database;

import java.util.UUID;

public class HomeData {
    private final UUID uuid;
    private final String ownerName;
    private final String name;
    private final String world;
    private final double x, y, z;
    private final float yaw, pitch;

    public HomeData(UUID uuid, String ownerName, String name, String world, double x, double y, double z, float yaw, float pitch) {
        this.uuid = uuid; this.ownerName = ownerName; this.name = name; this.world = world;
        this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
    }

    public HomeData(UUID uuid, String world, double x, double y, double z, float yaw, float pitch) {
        this(uuid, null, "home", world, x, y, z, yaw, pitch);
    }
    public UUID getUuid() { return uuid; }
    public String getOwnerName() { return ownerName; }
    public String getName() { return name; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}
