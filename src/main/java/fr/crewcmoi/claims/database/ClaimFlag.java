package fr.crewcmoi.claims.database;

/**
 * Une règle configurable au sein d'un claim (ex : casser des blocs, ouvrir des coffres...).
 * Chaque règle a un niveau de permission indépendant (voir {@link ClaimPermission}).
 */
public enum ClaimFlag {

    BUILD("Construire", "Poser des blocs"),
    BREAK("Détruire", "Casser des blocs"),
    CONTAINERS("Conteneurs", "Ouvrir coffres, fours, tonneaux, enclumes..."),
    INTERACT("Interactions", "Portes, leviers, boutons, lits..."),
    BUCKETS("Seaux", "Utiliser un seau (eau, lave...)"),
    FIRE("Feu", "Allumer/laisser se propager le feu"),
    EXPLOSIONS("Explosions", "TNT, creepers... (dégâts aux blocs)"),
    MOB_GRIEFING("Mobs", "Modification de blocs par les monstres/animaux");

    private final String displayName;
    private final String description;

    ClaimFlag(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
