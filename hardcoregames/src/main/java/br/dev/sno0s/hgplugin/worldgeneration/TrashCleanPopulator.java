package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.HeightMap;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import java.util.Random;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TrashCleanPopulator extends BlockPopulator implements Listener {

    public static final Set<Material> CLEAR_BLOCKS = new HashSet<>(Arrays.asList(
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.LILY_OF_THE_VALLEY,
            Material.SHORT_GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.PEONY,
            Material.SUNFLOWER,
            Material.DANDELION,
            Material.POPPY,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.RED_TULIP,
            Material.ORANGE_TULIP,
            Material.WHITE_TULIP,
            Material.PINK_TULIP,
            Material.OXEYE_DAISY,
            Material.CORNFLOWER,
            Material.WITHER_ROSE,
            Material.TORCHFLOWER,
            Material.PITCHER_PLANT,
            Material.DEAD_BUSH,
            Material.LEAF_LITTER,
            Material.WILDFLOWERS
    ));

    // Limpa um chunk — pode ser chamado tanto pelo evento quanto manualmente
    @Override
    public void populate(WorldInfo world, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        for (int x = chunkX << 4; x < (chunkX << 4) + 16; x++) {
            for (int z = chunkZ << 4; z < (chunkZ << 4) + 16; z++) {
                for (int y = region.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE); y > world.getMinHeight(); y--) {
                    Material type = region.getType(x, y, z);
                    if (CLEAR_BLOCKS.contains(type)) region.setType(x, y, z, Material.AIR);
                    else if (type.isSolid() && !SurfaceBlocks.isCanopy(type)) break;
                }
            }
        }
    }

    public static void cleanChunk(Chunk chunk) {
        World world = chunk.getWorld();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (chunk.getX() << 4) + x;
                int worldZ = (chunk.getZ() << 4) + z;

                int highestY = world.getHighestBlockYAt(worldX, worldZ);

                for (int y = highestY; y > world.getMinHeight(); y--) {
                    Block block = world.getBlockAt(worldX, y, worldZ);
                    Material type = block.getType();

                    if (CLEAR_BLOCKS.contains(type)) {
                        block.setType(Material.AIR, false);
                    } else if (type.isSolid() && !SurfaceBlocks.isCanopy(type)) break;
                }
            }
        }
    }

    // Chunks carregados por players ao caminhar — delay de 1 tick para o Paper
    // terminar de aplicar decorações antes de limpar
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.getWorld().getName().equals("hg_world")) return;

        Chunk chunk = event.getChunk();
        World sourceWorld = event.getWorld();
        Bukkit.getScheduler().runTask(Hgplugin.getInstance(), () -> {
            // Discovery/reset can unload this world before the next tick.
            if (Bukkit.getWorld(sourceWorld.getUID()) == sourceWorld && chunk.isLoaded()) {
                cleanChunk(chunk);
            }
        });
    }
}
