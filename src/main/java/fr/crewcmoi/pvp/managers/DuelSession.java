package fr.crewcmoi.pvp.managers;

import java.util.UUID;

/**
 * Représente un duel en cours entre deux joueurs (créé après acceptation d'une
 * demande de /duel). Contient les règles choisies par le demandeur (keepinventory,
 * argent en jeu) ainsi que l'emplacement d'origine des deux joueurs, pour pouvoir
 * les y renvoyer une fois le duel terminé.
 */
public class DuelSession {

    private final UUID player1;
    private final UUID player2;
    private final boolean keepInventory;
    private final double bet;
    private final boolean dropHead;

    // Position avant téléportation dans l'arène, pour ramener les joueurs après le duel.
    private org.bukkit.Location originLocation1;
    private org.bukkit.Location originLocation2;

    // Le duel n'inflige/ne prend en compte les dégâts qu'une fois le compte à rebours terminé.
    private boolean started = false;

    public DuelSession(UUID player1, UUID player2, boolean keepInventory, double bet, boolean dropHead) {
        this.player1 = player1;
        this.player2 = player2;
        this.keepInventory = keepInventory;
        this.bet = bet;
        this.dropHead = dropHead;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public UUID getOpponent(UUID player) {
        if (player.equals(player1)) {
            return player2;
        } else if (player.equals(player2)) {
            return player1;
        }
        return null;
    }

    public boolean involves(UUID player) {
        return player.equals(player1) || player.equals(player2);
    }

    public boolean isKeepInventory() {
        return keepInventory;
    }

    public double getBet() {
        return bet;
    }

    public boolean isDropHead() {
        return dropHead;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public org.bukkit.Location getOriginLocation1() {
        return originLocation1;
    }

    public void setOriginLocation1(org.bukkit.Location originLocation1) {
        this.originLocation1 = originLocation1;
    }

    public org.bukkit.Location getOriginLocation2() {
        return originLocation2;
    }

    public void setOriginLocation2(org.bukkit.Location originLocation2) {
        this.originLocation2 = originLocation2;
    }

    public org.bukkit.Location getOriginLocation(UUID player) {
        if (player.equals(player1)) {
            return originLocation1;
        } else if (player.equals(player2)) {
            return originLocation2;
        }
        return null;
    }
}
