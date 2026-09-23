package br.dev.sno0s.hgplugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Machado infinito do Lumberjack; sua identidade não depende do nome visível. */
public final class LumberjackAxe {
    private LumberjackAxe() {}

    /** Nome e lore vêm do bloco items.lumberjack, o mesmo do ícone no menu de kits. */
    public static ItemStack create() {
        ItemStack axe = PluginItems.create(Material.WOODEN_AXE, "lumberjack-axe", "items.lumberjack");
        ItemMeta meta = axe.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            axe.setItemMeta(meta);
        }
        return axe;
    }
}
