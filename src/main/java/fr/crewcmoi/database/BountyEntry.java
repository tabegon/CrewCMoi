package fr.crewcmoi.database;

import java.util.UUID;

/**
 * Représente une contribution à la prime d'un joueur (target).
 * Le contributeur peut être un joueur, ou le serveur lui-même
 * (contributorUuid == null, contributorName == "Serveur") lorsque la prime
 * est attribuée automatiquement suite à une agression injustifiée.
 */
public class BountyEntry {

    private final int id;
    private final UUID targetUuid;
    private final String targetName;
    private final UUID contributorUuid; // null si prime serveur
    private final String contributorName;
    private final double amount;
    private final long createdAt;

    public BountyEntry(int id, UUID targetUuid, String targetName, UUID contributorUuid,
                        String contributorName, double amount, long createdAt) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.contributorUuid = contributorUuid;
        this.contributorName = contributorName;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getContributorUuid() {
        return contributorUuid;
    }

    public String getContributorName() {
        return contributorName;
    }

    public double getAmount() {
        return amount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isServerBounty() {
        return contributorUuid == null;
    }
}
