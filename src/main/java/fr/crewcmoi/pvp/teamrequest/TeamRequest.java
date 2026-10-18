package fr.crewcmoi.pvp.teamrequest;

public record TeamRequest(int id, int teamId, String type, String material, String itemData, double target, double collected, boolean active) {
    public double remaining() { return Math.max(0.0, target - collected); }
}
