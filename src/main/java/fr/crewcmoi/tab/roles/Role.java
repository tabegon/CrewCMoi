package fr.crewcmoi.tab.roles;

/**
 * Les différents rôles/rangs affichables dans le tab et au-dessus de la tête des
 * joueurs. Chaque rôle a :
 *  - un identifiant (utilisé en config.yml et dans les commandes, ex: "fonda") ;
 *  - un préfixe et une couleur par défaut (personnalisables en config.yml, voir
 *    RoleManager) ;
 *  - un "weight" qui détermine l'ordre d'affichage dans le tab, du plus haut rang
 *    (0 = en haut de la liste) au plus bas.
 *
 * L'ordre des constantes ci-dessous correspond directement à l'ordre d'affichage.
 */
public enum Role {

    FONDA("fonda", "&6- Fonda &f", 0),
    ADMIN("admin", "&4- Admin &f", 1),
    DEV("dev", "&d- Dev &f", 2),
    MOD("mod", "&9- Mod &f", 3),
    VIP("vip", "&a- Vip &f", 4),
    PLAYER("player", "&7- Player &f", 5);

    private final String id;
    private final String defaultPrefix;
    private final int weight;

    Role(String id, String defaultPrefix, int weight) {
        this.id = id;
        this.defaultPrefix = defaultPrefix;
        this.weight = weight;
    }

    /**
     * Identifiant utilisé en config.yml (section "roles.<id>") et dans les
     * commandes (ex: /rank set Steve dev).
     */
    public String getId() {
        return id;
    }

    /**
     * Préfixe par défaut (codes couleur '&'), utilisé si aucune valeur n'est
     * définie en config.yml pour ce rôle. Voir RoleManager#getPrefix.
     */
    public String getDefaultPrefix() {
        return defaultPrefix;
    }

    /**
     * Position dans le tab : 0 = affiché en premier (en haut). Détermine aussi
     * l'ordre de priorité utilisé par RoleManager quand un joueur a plusieurs
     * permissions de rôle à la fois (le rôle avec le plus petit weight l'emporte).
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Rôle le plus bas, attribué par défaut à un joueur sans rôle particulier.
     */
    public static Role getDefault() {
        return PLAYER;
    }

    /**
     * Vrais rôles de staff pouvant utiliser /staff (voir StaffModeManager) :
     * Fonda, Admin, Dev et Mod. Vip et Player ne sont pas concernés.
     */
    public boolean isStaffRole() {
        return this == FONDA || this == ADMIN || this == DEV || this == MOD;
    }

    /**
     * Retrouve un rôle à partir de son identifiant (insensible à la casse),
     * ou null s'il n'existe pas.
     */
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
