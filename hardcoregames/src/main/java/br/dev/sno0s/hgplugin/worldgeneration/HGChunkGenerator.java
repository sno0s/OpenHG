package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Random;

/** Gentle classic HG terrain; vanilla still supplies caves, ores and biome features. */
public final class HGChunkGenerator extends ChunkGenerator {
    private final TerrainProfile terrain;
    private final List<BlockPopulator> populators;

    public HGChunkGenerator(TerrainProfile terrain, int treeDensity, int mushroomDensity) {
        this.terrain = terrain;
        populators = List.of(new TreePopulator(treeDensity), new MushroomPopulator(mushroomDensity));
    }

    @Override
    public void generateNoise(WorldInfo world, Random random, int chunkX, int chunkZ, ChunkData data) {
        int minY = data.getMinHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = surfaceHeight(world, (chunkX << 4) + x, (chunkZ << 4) + z);
                data.setBlock(x, minY, z, Material.BEDROCK);
                int stoneStart = minY + 1;
                if (stoneStart < 0) {
                    int deepEnd = Math.min(0, top - 3);
                    data.setRegion(x, stoneStart, z, x + 1, deepEnd, z + 1, Material.DEEPSLATE);
                    stoneStart = deepEnd;
                }
                data.setRegion(x, stoneStart, z, x + 1, top - 3, z + 1, Material.STONE);
                data.setRegion(x, top - 3, z, x + 1, top, z + 1, Material.DIRT);
                data.setBlock(x, top, z, Material.GRASS_BLOCK);
            }
        }
    }

    private int surfaceHeight(WorldInfo world, int x, int z) {
        return Math.clamp(terrain.heightAt(world.getSeed(), x, z), world.getMinHeight() + 5, world.getMaxHeight() - 32);
    }

    @Override
    public int getBaseHeight(WorldInfo world, Random random, int x, int z, HeightMap heightMap) {
        return surfaceHeight(world, x, z) + 1;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        // Registered before spawn chunks are generated.
        return populators;
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return true; }
    @Override public boolean shouldGenerateDecorations() { return true; }
    @Override public boolean shouldGenerateMobs() { return true; }
    @Override public boolean shouldGenerateStructures() { return false; }
    @Override public boolean canSpawn(World world, int x, int z) { return Math.abs(x) <= 10 && Math.abs(z) <= 10; }
}
