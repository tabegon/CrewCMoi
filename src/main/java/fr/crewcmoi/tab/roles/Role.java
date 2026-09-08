package fr.crewcmoi.tab.roles;

public enum Role {

    FONDA("fonda", "§f\uE509 ", 0),
    DEV("dev", "§f\uE508 ", 1),
    MOD("mod", "§f\uE507 ", 2),
    VIP("vip", "§f\uE510 ", 3),
    PLAYER("player", "§f\uE518 ", 4);

    private final String id;
    private final String defaultPrefix;
    private final int weight;

    Role(String id, String defaultPrefix, int weight) {
        this.id = id;
        this.defaultPrefix = defaultPrefix;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public String getDefaultPrefix() {
        return defaultPrefix;
    }

    public int getWeight() {
        return weight;
    }

    public static Role getDefault() {
        return PLAYER;
    }

    public boolean isStaffRole() {
        return this == FONDA || this == DEV || this == MOD;
    }

    public static Role fromId(String id) {
        if (id == null) {
            return null;
        }
        for (Role role : values()) {
            if (role.id.equalsIgnoreCase(id)) {
                return role;
            }
        }
        return null;
    }
}
