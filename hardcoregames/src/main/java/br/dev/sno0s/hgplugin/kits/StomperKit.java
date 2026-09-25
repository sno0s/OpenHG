package br.dev.sno0s.hgplugin.kits;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Kit passivo: o impacto da queda é tratado pelo StomperListener. */
public final class StomperKit extends Kit {
    @Override public String getName() { return "Stomper"; }
    @Override public ItemStack getIconMaterial() { return new ItemStack(Material.IRON_BOOTS); }
    @Override public void apply(Player player) { /* passivo, sem item */ }
}
