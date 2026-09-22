package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.items.PluginItems;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class OnDrop implements Listener {

    private final Set<String> blockedItems = Set.of("kit-selector", "stats", "rocket", "lumberjack-axe");

    @EventHandler
    public void onOffHandUse(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) event.setCancelled(true);
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (item == null || !item.hasItemMeta()) return;

        String itemId = PluginItems.id(item);

        if (itemId != null && blockedItems.contains(itemId)) {
            event.setCancelled(true);
            Messages.error(event.getPlayer(), "items.cannot-drop");
        }
    }
}
