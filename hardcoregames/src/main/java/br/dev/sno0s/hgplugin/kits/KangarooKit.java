package br.dev.sno0s.hgplugin.kits;

import br.dev.sno0s.hgplugin.items.Rocket;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KangarooKit extends Kit {

    @Override
    public String getName() {
        return "Kangaroo";
    }

    @Override
    public String getDescription() {
        return "Use o foguete para saltar ou avançar!";
    }

    @Override
    public ItemStack getIconMaterial() {
        return new ItemStack(Material.FIREWORK_ROCKET);
    }

    @Override
    public void apply(Player player) {
        player.getInventory().setItem(0, Rocket.create());
    }
}
