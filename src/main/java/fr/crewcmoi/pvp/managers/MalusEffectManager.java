package fr.crewcmoi.pvp.managers;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MalusEffectManager {

    private final Main plugin;

    private final double tier1Threshold;
    private final double tier1ReductionPercent;
    private final double tier2Threshold;
    private final double tier2ReductionPercent;
    private final double tier3Threshold;
    private final double tier3ReductionPercent;

    private final double heartRemovalStartThreshold;
    private final double heartRemovalStep;
    private final int maxHeartsRemoved;
    private final double baseMaxHealth;

    
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
            Messages.send(player, "bounty.malus.removed");
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
            Messages.send(player, "bounty.malus.health-reduced");
        } else {
            Messages.send(player, "bounty.malus.health-restored");
        }
    }

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

    public double getReductionPercentForLevel(int level) {
        return switch (level) {
            case 1 -> tier1ReductionPercent;
            case 2 -> tier2ReductionPercent;
            case 3 -> tier3ReductionPercent;
            default -> 0.0;
        };
    }

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

    public void clearVolatileState(Player player) {
        reductionLevel.remove(player.getUniqueId());
    }

    private void sendMessage(Player player, String path) {
        String message = plugin.getMessages().getString(path);
        if (message == null) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix");
        player.sendMessage((prefix + message).replace('&', '§'));
    }
}
