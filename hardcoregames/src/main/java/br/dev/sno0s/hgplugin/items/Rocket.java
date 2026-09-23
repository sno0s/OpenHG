package br.dev.sno0s.hgplugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class Rocket {

    /** Foguete do kit Kangaroo; nome e lore vêm do bloco items.kangaroo. */
    public static ItemStack create() {
        return PluginItems.create(Material.FIREWORK_ROCKET, "rocket", "items.kangaroo");
    }

    private Rocket() {}
}
