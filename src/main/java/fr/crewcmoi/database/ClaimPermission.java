package fr.crewcmoi.database;

/**
 * Détermine qui est autorisé à effectuer une action régie par un {@link ClaimFlag}
 * sur un claim, en plus du propriétaire (toujours autorisé).
 */
public enum ClaimPermission {

    /** Seul le propriétaire peut faire cette action (même les joueurs de confiance sont bloqués). */
    OWNER_ONLY("Propriétaire uniquement"),

    /** Le propriétaire et les joueurs de confiance peuvent faire cette action. (Valeur par défaut) */
    TRUSTED("Propriétaire + joueurs de confiance"),

    /** Tout le monde peut faire cette action, la règle n'est pas restreinte. */
    EVERYONE("Tout le monde");

    private final String displayName;

    ClaimPermission(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Passe au niveau suivant (utilisé pour le clic dans la GUI de settings) :
     * OWNER_ONLY -> TRUSTED -> EVERYONE -> OWNER_ONLY -> ...
     */
    public ClaimPermission next() {
        ClaimPermission[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
