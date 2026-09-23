package br.dev.sno0s.hgplugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class KitSelector {

    /** Item que abre o menu de kits; nome e lore vêm do items.yml. */
    public static ItemStack create() {
        return PluginItems.create(Material.CHEST, "kit-selector");
    }

    private KitSelector() {}
}
