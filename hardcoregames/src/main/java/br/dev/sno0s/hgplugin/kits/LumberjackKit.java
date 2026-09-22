package br.dev.sno0s.hgplugin.kits;

import br.dev.sno0s.hgplugin.items.LumberjackAxe;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LumberjackKit extends Kit {
    @Override public String getName() { return "Lumberjack"; }
    @Override public String getDescription() { return Messages.text("kits.lumberjack.description"); }
    @Override public ItemStack getIconMaterial() { return new ItemStack(Material.WOODEN_AXE); }
    @Override public void apply(Player player) { player.getInventory().setItem(0, LumberjackAxe.create()); }
}
