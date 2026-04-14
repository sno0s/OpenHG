package br.dev.sno0s.hgplugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class StatsItem {

    public static final String DISPLAY_NAME = "§aEstatísticas";

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(DISPLAY_NAME);
            meta.setLore(List.of("§7Clique para ver suas estatísticas!"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
