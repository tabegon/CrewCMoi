package fr.crewcmoi.utils;

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
}
