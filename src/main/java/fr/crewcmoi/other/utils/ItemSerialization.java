package fr.crewcmoi.other.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utilitaire de (dé)sérialisation d'ItemStack en Base64, pour stockage en base de données
 * (ex : objets mis en vente dans l'hôtel des ventes).
 */
public final class ItemSerialization {

    private ItemSerialization() {
    }

    public static String toBase64(ItemStack item) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(byteStream)) {
            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        }
    }

    public static ItemStack fromBase64(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(byteStream)) {
            return (ItemStack) dataInput.readObject();
        }
    }

    /**
     * Sérialise un tableau d'ItemStack complet (ex : le contenu d'un inventaire,
     * armure comprise) en une seule chaîne Base64. Utilisé pour sauvegarder/
     * restaurer l'inventaire d'un joueur (ex : /staff, voir StaffModeManager).
     */
    public static String toBase64(ItemStack[] items) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(byteStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        }
    }

    public static ItemStack[] itemArrayFromBase64(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(byteStream)) {
            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;
        }
    }
}
