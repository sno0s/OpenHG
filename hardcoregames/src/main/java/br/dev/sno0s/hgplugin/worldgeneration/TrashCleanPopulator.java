package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.HeightMap;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import java.util.Random;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TrashCleanPopulator extends BlockPopulator {

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

    // Limpeza inicial durante a geração; o listener repete no chunk pronto.
    @Override
    public void populate(WorldInfo world, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        for (int x = chunkX << 4; x < (chunkX << 4) + 16; x++) {
            for (int z = chunkZ << 4; z < (chunkZ << 4) + 16; z++) {
                for (int y = region.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE); y > world.getMinHeight(); y--) {
                    Material type = region.getType(x, y, z);
                    if (CLEAR_BLOCKS.contains(type)
                            || MushroomPopulator.isMushroom(type) && MushroomPopulator.isJungle(region.getBiome(x, y, z))) {
                        region.setType(x, y, z, Material.AIR);
                    } else if (type.isSolid() && !SurfaceBlocks.isCanopy(type)) {
                        break;
                    }
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

                int highestY = Math.min(world.getMaxHeight() - 1,
                        world.getHighestBlockYAt(worldX, worldZ, HeightMap.WORLD_SURFACE));

                for (int y = highestY; y > world.getMinHeight(); y--) {
                    Block block = world.getBlockAt(worldX, y, worldZ);
                    Material type = block.getType();

                    if (CLEAR_BLOCKS.contains(type)
                            || MushroomPopulator.isMushroom(type) && MushroomPopulator.isJungle(block.getBiome())) {
                        block.setType(Material.AIR, false);
                    } else if (type.isSolid() && !SurfaceBlocks.isCanopy(type)) {
                        break;
                    }
                }
            }
        }
    }

}
