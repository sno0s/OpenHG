package br.dev.sno0s.hgplugin.items;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class Rocket {


    public static ItemStack create() {

        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(Messages.text("items.rocket.name"));
            PluginItems.mark(meta, "rocket");
            meta.setLore(Messages.lines("items.rocket.lore"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private Rocket() {}
}
