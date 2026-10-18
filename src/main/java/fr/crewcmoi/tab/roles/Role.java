package fr.crewcmoi.tab.roles;

public enum Role {

    FONDA("fonda", "§f\uE509 ", 1000, 1000),
    DEV("dev", "§f\uE508 ", 900, 900),
    MOD("mod", "§f\uE507 ", 800, 800),
    VIP("vip", "§f\uE510 ", 700, 700),
    YOUTUBEUR("youtubeur", "§f\uE533 ", 600, 600),
    TESTER("tester", "§f\uE534 ", 500, 500),
    PLAYER("player", "§f\uE518 ", 100, 100);

    private final String id;
    private final String defaultPrefix;
    private final int weight;
    private final int priority;

    Role(String id, String defaultPrefix, int weight, int priority) {
        this.id = id;
        this.defaultPrefix = defaultPrefix;
        this.weight = weight;
        this.priority = priority;
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

    public int getPriority() {
        return priority;
    }

    public static Role getDefault() {
        return PLAYER;
    }

    public boolean isStaffRole() {
        return this == FONDA || this == DEV || this == MOD;
    }

    public static Role fromId(String id) {
        if (id == null) return null;
        for (Role role : values()) {
            if (role.id.equalsIgnoreCase(id)) return role;
        }
        return null;
    }
}
