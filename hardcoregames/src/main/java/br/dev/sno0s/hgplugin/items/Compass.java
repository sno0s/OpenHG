package br.dev.sno0s.hgplugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class Compass {

    /** Bússola que aponta para o jogador mais próximo; nome e lore vêm do items.yml. */
    public static ItemStack create() {
        return PluginItems.create(Material.COMPASS, "compass");
    }

    private Compass() {}
}
