package br.dev.sno0s.hgplugin.listeners;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class EnchantListener implements Listener {

    @EventHandler
    public void onEnchantClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ENCHANTING) return;
        // Slot 1 = lapis — bloqueia qualquer interação com ele
        if (event.getRawSlot() == 1) event.setCancelled(true);
    }

    @EventHandler
    public void onEnchantOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.ENCHANTING) return;

        HumanEntity player = event.getPlayer();
        if (!player.getWorld().getName().equals("hg_world")) return;

        // Slot 1 da enchanting table = slot do lapis
        ItemStack lapis = event.getInventory().getItem(1);
        int current = (lapis != null && lapis.getType() == Material.LAPIS_LAZULI)
                ? lapis.getAmount() : 0;

        if (current < 64) {
            event.getInventory().setItem(1, new ItemStack(Material.LAPIS_LAZULI, 64));
        }
    }
}
