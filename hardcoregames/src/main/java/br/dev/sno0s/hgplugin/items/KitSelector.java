package br.dev.sno0s.hgplugin.items;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class KitSelector {

    private static final Material ITEM_MATERIAL = Material.CHEST;

    /*
        Cria e retorna o item configurado do Kit Selector

        TODO: change deprecated methods
     */
    public static ItemStack create() {
        ItemStack item = new ItemStack(ITEM_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Messages.text("items.kit-selector.name"));
            PluginItems.mark(meta, "kit-selector");
            meta.setLore(Messages.lines("items.kit-selector.lore"));
            item.setItemMeta(meta);
        }

        return item;
    }

    private KitSelector() {}
}
