package br.dev.sno0s.hgplugin.items;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Identidade interna dos itens, sem depender do nome exibido. */
public final class PluginItems {
    private static final NamespacedKey KEY = new NamespacedKey("hardcoregames", "item");

    private PluginItems() {}

    /**
     * Cria um item do plugin com nome e lore vindos do items.yml.
     *
     * @param id          identidade persistente, independente do nome exibido
     * @param catalogKey  bloco do items.yml com name e lore (ex: items.compass)
     */
    public static ItemStack create(Material material, String id, String catalogKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.text(catalogKey + ".name"));
            mark(meta, id);
            meta.setLore(Messages.lines(catalogKey + ".lore"));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Item cujo bloco no items.yml tem o mesmo nome da identidade. */
    public static ItemStack create(Material material, String id) {
        return create(material, id, "items." + id);
    }

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
