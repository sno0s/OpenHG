package br.dev.sno0s.hgplugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureSpawnEvent;

public class ArenaStructureListener implements Listener {

    @EventHandler
    public void onStructureSpawn(AsyncStructureSpawnEvent event) {
        if (!event.getWorld().getName().equals("hg_world")) return;

        // O evento é assíncrono: consulta apenas o identificador, sem acessar blocos do mundo.
        String structure = event.getStructure().key().value();
        if (!structure.startsWith("village_") && !structure.equals("desert_pyramid")) {
            event.setCancelled(true);
        }
    }
}
