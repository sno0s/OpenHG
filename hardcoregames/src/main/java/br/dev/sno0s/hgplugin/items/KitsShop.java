package br.dev.sno0s.hgplugin.items;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class KitsShop {

    private static final Material ITEM_MATERIAL = Material.EMERALD;

    /*
        create the shop item
     */
    public static ItemStack create() {
        ItemStack item = new ItemStack(ITEM_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Messages.text("items.kit-shop.name"));
            PluginItems.mark(meta, "kit-shop");
            meta.setLore(Messages.lines("items.kit-shop.lore"));
            item.setItemMeta(meta);
        }

        return item;
    }
}
