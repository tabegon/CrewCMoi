package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les effets de malus liés à la prime attribuée par le SERVEUR ("prime du serveur",
 * voir BountyEntry#isServerBounty) sur un joueur :
 *  - dès que le montant de sa prime serveur atteint un premier palier, le joueur inflige
 *    moins de dégâts aux autres joueurs (effet "malchance" codé en dur, pas un PotionEffect
 *    vanilla, ne touche que les dégâts contre des joueurs) ;
 *  - à partir d'un second palier plus élevé, la réduction de dégâts est encore plus forte ;
 *  - à partir d'un troisième palier, puis tous les paliers suivants d'un même montant,
 *    un coeur de vie maximum est retiré de façon PERMANENTE au joueur (jusqu'à un nombre
 *    maximum configurable), et ce jusqu'à ce que sa prime serveur retombe sous le palier
 *    correspondant (typiquement quand il perd sa prime, càd qu'il est tué).
 *
 * Contrairement à l'ancienne version de ce système, ces effets ne dépendent plus d'une
 * durée écoulée : ils sont entièrement déterminés par le montant ACTUEL de la prime
 * serveur du joueur, et recalculés à chaque changement de celle-ci (voir BountyManager,
 * appelé à l'ajout d'une prime serveur, à la réclamation/perte de la prime, et à la
 * connexion du joueur).
 */
public class MalusEffectManager {

    private final Main plugin;

    // --- Paliers de réduction de dégâts ---
    private final double tier1Threshold;
    private final double tier1ReductionPercent;
    private final double tier2Threshold;
    private final double tier2ReductionPercent;
    private final double tier3Threshold;
    private final double tier3ReductionPercent;

    // --- Palier de retrait de coeurs ---
    private final double heartRemovalStartThreshold;
    private final double heartRemovalStep;
    private final int maxHeartsRemoved;
    private final double baseMaxHealth;

    // Niveau de réduction de dégâts actuellement applicable à chaque joueur en ligne
    // (0 = aucune, 1 = palier 1, 2 = palier 2). Absent = 0.
    private final Map<UUID, Integer> reductionLevel = new ConcurrentHashMap<>();

    public MalusEffectManager(Main plugin) {
        this.plugin = plugin;

        this.tier1Threshold = plugin.getConfig().getDouble("bounty.bad-luck.tier1-threshold", 50.0);
        this.tier1ReductionPercent = clamp(plugin.getConfig()
                .getDouble("bounty.bad-luck.tier1-damage-reduction-percent", 15.0) / 100.0);

        this.tier2Threshold = plugin.getConfig().getDouble("bounty.bad-luck.tier2-threshold", 100.0);
        this.tier2ReductionPercent = clamp(plugin.getConfig()
                .getDouble("bounty.bad-luck.tier2-damage-reduction-percent", 35.0) / 100.0);

        this.tier3Threshold = plugin.getConfig().getDouble("bounty.bad-luck.tier3-threshold", 150.0);
        this.tier3ReductionPercent = clamp(plugin.getConfig()
                .getDouble("bounty.bad-luck.tier3-damage-reduction-percent", 50.0) / 100.0);

        this.heartRemovalStartThreshold = plugin.getConfig()
                .getDouble("bounty.bad-luck.heart-removal-start-threshold", 200.0);
        this.heartRemovalStep = Math.max(1.0, plugin.getConfig()
                .getDouble("bounty.bad-luck.heart-removal-step", 100.0));
        this.maxHeartsRemoved = Math.max(0, plugin.getConfig()
                .getInt("bounty.bad-luck.max-hearts-removed", 5));
        this.baseMaxHealth = plugin.getConfig().getDouble("bounty.bad-luck.base-max-health", 20.0);
    }

    private double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }

    /**
     * Recalcule et applique l'intégralité des effets de malus (réduction de dégâts +
     * coeurs retirés) d'un joueur en fonction du montant ACTUEL de sa prime serveur.
     * Idempotent : peut être appelé à chaque changement de prime sans effet de bord si
     * rien n'a changé.
     */
    public void updateBountyEffect(Player player, double serverBountyTotal) {
        updateDamageReduction(player, serverBountyTotal);
        updateHeartRemoval(player, serverBountyTotal);
        updateBadLuck(player, serverBountyTotal);
    }

    private void updateBadLuck(Player player, double serverBountyTotal) {
        if (serverBountyTotal > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.UNLUCK,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    true
            ));
        } else {
            player.removePotionEffect(PotionEffectType.UNLUCK);
        }
    }

    private void updateDamageReduction(Player player, double serverBountyTotal) {
        int newLevel;
        if (serverBountyTotal >= tier3Threshold) {
            newLevel = 3;
        } else if (serverBountyTotal >= tier2Threshold) {
            newLevel = 2;
        } else if (serverBountyTotal >= tier1Threshold) {
            newLevel = 1;
        } else {
            newLevel = 0;
        }

        int previousLevel = reductionLevel.getOrDefault(player.getUniqueId(), 0);
        if (newLevel == previousLevel) {
            return;
        }

        if (newLevel == 0) {
            reductionLevel.remove(player.getUniqueId());
            sendMessage(player, "bounty.bad-luck-ended");
        } else {
            reductionLevel.put(player.getUniqueId(), newLevel);
            sendMessage(player, newLevel == 2 ? "bounty.bad-luck-tier2" : "bounty.bad-luck-tier1");
        }
    }

    private int heartsForAmount(double serverBountyTotal) {
        if (maxHeartsRemoved <= 0 || serverBountyTotal < heartRemovalStartThreshold) {
            return 0;
        }
        int hearts = 1 + (int) Math.floor((serverBountyTotal - heartRemovalStartThreshold) / heartRemovalStep);
        return Math.min(maxHeartsRemoved, hearts);
    }

    private void updateHeartRemoval(Player player, double serverBountyTotal) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute == null) {
            return;
        }

        int heartsToRemove = heartsForAmount(serverBountyTotal);
        double targetMax = Math.max(2.0, baseMaxHealth - (heartsToRemove * 2.0));
        double previousMax = maxHealthAttribute.getBaseValue();

        if (previousMax == targetMax) {
            return;
        }

        maxHealthAttribute.setBaseValue(targetMax);
        if (player.getHealth() > targetMax) {
            player.setHealth(targetMax);
        }

        if (targetMax < previousMax) {
            sendMessage(player, "bounty.bad-luck-heart-removed");
        } else {
            sendMessage(player, "bounty.bad-luck-heart-restored");
        }
    }

    /**
     * Applique la réduction de dégâts à un montant de dégâts donné si l'attaquant est
     * actuellement affecté par le malus de prime serveur. Ne réduit que les coups
     * portés contre d'autres joueurs (vérifié par l'appelant, voir CombatListener).
     */
    public double applyReduction(Player attacker, double damage) {
        int level = reductionLevel.getOrDefault(attacker.getUniqueId(), 0);
        if (level == 0) {
            return damage;
        }
        double reduction;

        if (level == 3) {
            reduction = tier3ReductionPercent;
        } else if (level == 2) {
            reduction = tier2ReductionPercent;
        } else {
            reduction = tier1ReductionPercent;
        }

        return damage * (1.0 - reduction);
    }

    public boolean isAffected(Player player) {
        return reductionLevel.getOrDefault(player.getUniqueId(), 0) > 0;
    }

    /**
     * Calcule le palier de réduction de dégâts (0 à 3) correspondant à un montant de
     * prime serveur donné, indépendamment du fait que le joueur soit en ligne ou non
     * (utilisé par /info bounty pour afficher l'état d'un joueur hors-ligne).
     */
    public int computeReductionLevel(double serverBountyTotal) {
        if (serverBountyTotal >= tier3Threshold) {
            return 3;
        }
        if (serverBountyTotal >= tier2Threshold) {
            return 2;
        }
        if (serverBountyTotal >= tier1Threshold) {
            return 1;
        }
        return 0;
    }

    /**
     * Pourcentage de réduction de dégâts appliqué pour un palier donné (0 à 3).
     */
    public double getReductionPercentForLevel(int level) {
        return switch (level) {
            case 1 -> tier1ReductionPercent;
            case 2 -> tier2ReductionPercent;
            case 3 -> tier3ReductionPercent;
            default -> 0.0;
        };
    }

    /**
     * Calcule le nombre de coeurs actuellement retirés pour un montant de prime
     * serveur donné (indépendant du fait que le joueur soit en ligne ou non).
     */
    public int computeHeartsRemoved(double serverBountyTotal) {
        return heartsForAmount(serverBountyTotal);
    }

    public double getTier1Threshold() {
        return tier1Threshold;
    }

    public double getTier2Threshold() {
        return tier2Threshold;
    }

    public double getTier3Threshold() {
        return tier3Threshold;
    }

    public double getHeartRemovalStartThreshold() {
        return heartRemovalStartThreshold;
    }

    public double getHeartRemovalStep() {
        return heartRemovalStep;
    }

    public int getMaxHeartsRemoved() {
        return maxHeartsRemoved;
    }

    public double getBaseMaxHealth() {
        return baseMaxHealth;
    }

    /**
     * Retire l'état de réduction de dégâts gardé en mémoire pour un joueur (ex : à sa
     * déconnexion). Le nombre de coeurs retirés n'est PAS remis à zéro ici : c'est un
     * attribut du joueur, persisté par le serveur lui-même, qui sera recalculé/réajusté
     * si besoin à sa prochaine connexion (voir JoinListener).
     */
    public void clearVolatileState(Player player) {
        reductionLevel.remove(player.getUniqueId());
    }

    private void sendMessage(Player player, String path) {
        String message = plugin.getMessages().getString(path);
        if (message == null) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix", "");
        player.sendMessage((prefix + message).replace('&', '§'));
    }
}
