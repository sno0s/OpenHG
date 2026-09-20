package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.worldgeneration.CocoaPopulator;
import br.dev.sno0s.hgplugin.worldgeneration.MushroomPopulator;
import br.dev.sno0s.hgplugin.worldgeneration.TrashCleanPopulator;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class ChunkDecorationListener implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey decoratedKey;
    private final MushroomPopulator mushrooms;
    private final CocoaPopulator cocoa;

    public ChunkDecorationListener(JavaPlugin plugin, int mushroomDensity, int cocoaDensity) {
        this.plugin = plugin;
        decoratedKey = new NamespacedKey(plugin, "ground-decorated");
        mushrooms = new MushroomPopulator(mushroomDensity);
        cocoa = new CocoaPopulator(cocoaDensity);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        scheduleDecoration(event.getChunk());
    }

    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) {
        scheduleDecoration(event.getChunk());
    }

    private void scheduleDecoration(Chunk chunk) {
        World world = chunk.getWorld();
        if (!world.getName().equals("hg_world")) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            // O reset pode ter descarregado o mundo entre o evento e este tick.
            if (Bukkit.getWorld(world.getUID()) != world || !chunk.isLoaded()) return;
            decorateChunk(chunk);
        });
    }

    public void decorateChunk(Chunk chunk) {
        if (!chunk.getWorld().getName().equals("hg_world")) return;

        // A limpeza se repete ao carregar: decorações vizinhas também podem deixar folhas.
        TrashCleanPopulator.cleanChunk(chunk);
        if (chunk.getPersistentDataContainer().has(decoratedKey, PersistentDataType.BYTE)) return;

        World world = chunk.getWorld();
        Random random = new Random(world.getSeed() ^ (chunk.getX() * 341873128712L)
                ^ (chunk.getZ() * 132897987541L));
        mushrooms.populate(world, random, chunk);
        cocoa.populate(world, random, chunk);
        // Evita repor recursos coletados ao sair e voltar para o mesmo chunk.
        chunk.getPersistentDataContainer().set(decoratedKey, PersistentDataType.BYTE, (byte) 1);
    }
}
