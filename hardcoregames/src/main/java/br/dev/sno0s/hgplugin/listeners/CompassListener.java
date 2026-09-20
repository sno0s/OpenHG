package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.items.PluginItems;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import static br.dev.sno0s.hgplugin.utils.NearestPlayer.findNearestPlayer;

public class CompassListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        Player p = e.getPlayer();
        double raio = 150.0;

        if (item == null || item.getItemMeta() == null) return;
        if (!PluginItems.is(item, "compass")) return;

        Player nearest = findNearestPlayer(p.getLocation(), raio, p);

        if (nearest == null) {
            Messages.send(p, "compass.no-player", "radius", (int) raio);
            return;
        }

        Location loc = nearest.getLocation();
        p.setCompassTarget(loc);

        Messages.send(p, "compass.target", "player", nearest.getName(), "x", loc.getBlockX(), "y", loc.getBlockY(), "z", loc.getBlockZ());
    }
}
