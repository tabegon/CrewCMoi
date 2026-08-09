package fr.crewcmoi.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marqueur générique pour une GUI de confirmation (oui/non) affichée avant une
 * action sensible (achat à l'hôtel des ventes, vente d'objets, ...).
 * Les actions à exécuter sont fournies sous forme de Runnable au moment de
 * l'ouverture et ne servent que pour la durée de vie de cette GUI.
 */
public class ConfirmationHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int CANCEL_SLOT = 11;
    public static final int INFO_SLOT = 13;
    public static final int CONFIRM_SLOT = 15;

    private Inventory inventory;
    private Runnable onConfirm;
    private Runnable onCancel;

    // Empêche onCancel de s'exécuter une seconde fois (ex: clic sur "annuler"
    // qui ferme l'inventaire et déclenche ensuite InventoryCloseEvent).
    private boolean resolved = false;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public Runnable getOnConfirm() {
        return onConfirm;
    }

    public Runnable getOnCancel() {
        return onCancel;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
