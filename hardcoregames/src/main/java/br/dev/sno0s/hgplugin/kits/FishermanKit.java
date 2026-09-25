package br.dev.sno0s.hgplugin.kits;

import br.dev.sno0s.hgplugin.items.FishermanRod;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Teleporta o jogador fisgado ao recolher a vara, sem efeitos passivos. */
public final class FishermanKit extends Kit {
    @Override
    public String getName() { return "Fisherman"; }

    @Override
    public ItemStack getIconMaterial() { return new ItemStack(Material.FISHING_ROD); }

    @Override
    public void apply(Player player) {
        player.getInventory().setItem(0, FishermanRod.create());
    }
}
