package br.dev.sno0s.hgplugin.listeners;

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
        if (!item.getItemMeta().getDisplayName().contains("§eBússola")) return;

        Player nearest = findNearestPlayer(p.getLocation(), raio, p);

        if (nearest == null) {
            Messages.send(p, "Nenhum jogador encontrado dentro de " + Messages.hl((int) raio + " blocos") + ".");
            return;
        }

        Location loc = nearest.getLocation();
        p.setCompassTarget(loc);

        Messages.send(p, "Sua bússola aponta para " + Messages.hl(nearest.getName())
                + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
    }
}
