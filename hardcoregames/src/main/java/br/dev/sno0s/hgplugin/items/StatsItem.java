package br.dev.sno0s.hgplugin.items;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class StatsItem {


    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.text("items.stats.name"));
            PluginItems.mark(meta, "stats");
            meta.setLore(Messages.lines("items.stats.lore"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
