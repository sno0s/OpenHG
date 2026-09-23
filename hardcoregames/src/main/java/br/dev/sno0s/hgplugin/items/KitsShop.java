package br.dev.sno0s.hgplugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class KitsShop {

    /** Item da loja de kits; nome e lore vêm do items.yml. */
    public static ItemStack create() {
        return PluginItems.create(Material.EMERALD, "kit-shop");
    }

    private KitsShop() {}
}
