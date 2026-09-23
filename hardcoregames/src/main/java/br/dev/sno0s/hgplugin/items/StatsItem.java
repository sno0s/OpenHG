package br.dev.sno0s.hgplugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class StatsItem {

    /** Item que abre o menu de estatísticas; nome e lore vêm do items.yml. */
    public static ItemStack create() {
        return PluginItems.create(Material.EMERALD, "stats");
    }

    private StatsItem() {}
}
