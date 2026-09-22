package br.dev.sno0s.hgplugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Machado infinito do Lumberjack; sua identidade não depende do nome visível. */
public final class LumberjackAxe {
    private LumberjackAxe() {}

    public static ItemStack create() {
        ItemStack axe = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = axe.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            PluginItems.mark(meta, "lumberjack-axe");
            axe.setItemMeta(meta);
        }
        return axe;
    }
}
