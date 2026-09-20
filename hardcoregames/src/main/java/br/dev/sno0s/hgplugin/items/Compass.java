package br.dev.sno0s.hgplugin.items;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class Compass {

    private static final Material ITEM_MATERIAL = Material.COMPASS;

    /*
        Cria e retorna o item configurado do Kit Selector

        TODO: change deprecated methods
     */
    public static ItemStack create() {
        ItemStack item = new ItemStack(ITEM_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Messages.text("items.compass.name"));
            PluginItems.mark(meta, "compass");
            meta.setLore(Messages.lines("items.compass.lore"));
            item.setItemMeta(meta);
        }

        return item;
    }

    private Compass() {}
}
