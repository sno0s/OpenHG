package br.dev.sno0s.hgplugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class Rocket {

    public static final String DISPLAY_NAME = "§6Kangaroo";

    public static ItemStack create() {

        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(DISPLAY_NAME);
            meta.setLore(List.of(
                    "§7Clique para saltar!",
                    "§7Segure §eShift §7para ir para frente."
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private Rocket() {}
}
