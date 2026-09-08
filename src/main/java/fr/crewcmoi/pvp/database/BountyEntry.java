package fr.crewcmoi.pvp.database;

import java.util.UUID;

public class BountyEntry {

    private final int id;
    private final UUID targetUuid;
    private final String targetName;
    private final UUID contributorUuid; 
    private final String contributorName;
    private final double amount;
    private final long createdAt;
    private final String reason; 
    private final boolean approved; 

    public BountyEntry(int id, UUID targetUuid, String targetName, UUID contributorUuid,
                        String contributorName, double amount, long createdAt, String reason, boolean approved) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.contributorUuid = contributorUuid;
        this.contributorName = contributorName;
        this.amount = amount;
        this.createdAt = createdAt;
        this.reason = reason;
        this.approved = approved;
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

    public String getReason() {
        return reason;
    }

    public boolean hasReason() {
        return reason != null && !reason.isBlank();
    }

    public boolean isApproved() {
        return approved;
    }
}
