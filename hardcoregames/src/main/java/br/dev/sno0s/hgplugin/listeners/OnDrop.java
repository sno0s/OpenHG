package br.dev.sno0s.hgplugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class OnDrop implements Listener {

    /**
     *  this method blocks the drop of some items
     */

    // blocked items list
    private final Set<String> blockedItems = new HashSet<>(Arrays.asList(
            "§eSeletor de Kits",
            "§aLoja de kits"
    ));

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (item == null || !item.hasItemMeta()) return;

        String displayName = item.getItemMeta().getDisplayName();

        if (blockedItems.contains(displayName)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode dropar " + displayName + "!");
        }
    }

}
