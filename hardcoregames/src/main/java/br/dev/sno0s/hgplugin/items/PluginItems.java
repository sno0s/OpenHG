package br.dev.sno0s.hgplugin.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Identidade interna dos itens, sem depender do nome exibido. */
public final class PluginItems {
    private static final NamespacedKey KEY = new NamespacedKey("hardcoregames", "item");

    private PluginItems() {}

    public static void mark(ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, id);
    }

    public static String id(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    }

    public static boolean is(ItemStack item, String id) {
        return id.equals(id(item));
    }
}
