package fr.crewcmoi.claims.database;

public enum ClaimPermission {

    OWNER_ONLY("Propriétaire uniquement"),

    TRUSTED("Propriétaire + joueurs de confiance"),

    EVERYONE("Tout le monde");

    private final String displayName;

    ClaimPermission(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ClaimPermission next() {
        ClaimPermission[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
