package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Random;

/** Terreno de HG com cavernas, minérios e estruturas da geração vanilla. */
public final class HGChunkGenerator extends ChunkGenerator {
    private final TerrainProfile terrain;
    private final List<BlockPopulator> populators;
    private final double mobSpawnMultiplier;

    public HGChunkGenerator(TerrainProfile terrain, int treeDensity, double mobSpawnMultiplier) {
        this.terrain = terrain;
        this.mobSpawnMultiplier = mobSpawnMultiplier;
        populators = List.of(new TreePopulator(treeDensity), new TrashCleanPopulator());
    }

    @Override
    public void generateNoise(WorldInfo world, Random random, int chunkX, int chunkZ, ChunkData data) {
        int minY = data.getMinHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (chunkX << 4) + x;
                int worldZ = (chunkZ << 4) + z;
                int top = surfaceHeight(world, worldX, worldZ);
                boolean desert = terrain.biomeAt(world.getSeed(), worldX, worldZ) == TerrainProfile.Landscape.DESERT;
                data.setBlock(x, minY, z, Material.BEDROCK);
                int stoneStart = minY + 1;
                if (stoneStart < 0) {
                    int deepEnd = Math.min(0, top - 3);
                    data.setRegion(x, stoneStart, z, x + 1, deepEnd, z + 1, Material.DEEPSLATE);
                    stoneStart = deepEnd;
                }
                data.setRegion(x, stoneStart, z, x + 1, top - 3, z + 1, Material.STONE);
                data.setRegion(x, top - 3, z, x + 1, top, z + 1, desert ? Material.SANDSTONE : Material.DIRT);
                data.setBlock(x, top, z, desert ? Material.SAND : Material.GRASS_BLOCK);
            }
        }
    }

    @Override
    public void generateCaves(WorldInfo world, Random random, int chunkX, int chunkZ, ChunkData data) {
        RavineLayout ravines = new RavineLayout(world.getSeed(), terrain.worldSize());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (chunkX << 4) + x, worldZ = (chunkZ << 4) + z;
                int depth = ravines.depthAt(worldX, worldZ);
                if (depth == 0) continue;
                int top = surfaceHeight(world, worldX, worldZ);
                int bottom = Math.max(data.getMinHeight() + 1, top - depth + 1);
                data.setRegion(x, bottom, z, x + 1, top + 1, z + 1, Material.AIR);
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
        // Registrados antes da geração dos chunks iniciais.
        return populators;
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return true;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return true;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return true;
    }

    @Override
    public boolean shouldGenerateMobs(WorldInfo world, Random random, int chunkX, int chunkZ) {
        return random.nextDouble() < mobSpawnMultiplier;
    }
    @Override
    public boolean shouldGenerateStructures() {
        return true;
    }

    @Override
    public boolean canSpawn(World world, int x, int z) {
        return Math.abs(x) <= 10 && Math.abs(z) <= 10;
    }

}
