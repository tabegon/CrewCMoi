package fr.crewcmoi.pvp.database;

import java.util.UUID;

/**
 * Représente le total de prime cumulé sur un joueur, utilisé pour l'affichage
 * de la liste des primes dans la GUI /bounty.
 */
public class BountyTarget {

    private final UUID targetUuid;
    private final String targetName;
    private final double totalAmount;
    private final int contributorCount;

    public BountyTarget(UUID targetUuid, String targetName, double totalAmount, int contributorCount) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.totalAmount = totalAmount;
        this.contributorCount = contributorCount;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getContributorCount() {
        return contributorCount;
    }
}
