package br.dev.sno0s.hgplugin.kits;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Kit passivo: a habilidade é ativada ao pressionar Shift. */
public final class NinjaKit extends Kit {
    @Override public String getName() { return "Ninja"; }
    @Override public ItemStack getIconMaterial() { return new ItemStack(Material.EMERALD); }
    @Override public void apply(Player player) { /* passivo, sem item utilizável */ }
}
