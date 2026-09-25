package br.dev.sno0s.hgplugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Vara do Fisherman; nome e lore compartilham o catálogo do ícone do kit. */
public final class FishermanRod {
    private FishermanRod() {}

    public static ItemStack create() {
        ItemStack rod = PluginItems.create(Material.FISHING_ROD, "fisherman-rod", "items.fisherman");
        var meta = rod.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            rod.setItemMeta(meta);
        }
        return rod;
    }
}
