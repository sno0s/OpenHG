package br.dev.sno0s.hgplugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import static br.dev.sno0s.hgplugin.utils.NearestPlayer.findNearestPlayer;

public class CompassListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        Player p = e.getPlayer();
        Location origin = p.getLocation();
        double raio = 150.0;

        if(item.getItemMeta().getDisplayName().contains("§eBússola")) {
            Player nearest = findNearestPlayer(origin, raio, p);

            if (nearest == null) {
                p.sendMessage(ChatColor.YELLOW + "Nenhum jogador encontrado dentro de " + (int)raio + " blocos.");
                return;
            }

            Location loc = nearest.getLocation();
            p.setCompassTarget(loc);

            p.sendMessage("§aSua bússola agora aponta para §e"
                    + nearest.getName()
                    + " §7("
                    + loc.getBlockX() + ", "
                    + loc.getBlockY() + ", "
                    + loc.getBlockZ() + ")");
        }


    }
}
