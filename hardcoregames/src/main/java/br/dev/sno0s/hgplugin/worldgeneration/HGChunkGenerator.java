package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Material;
import org.bukkit.World;

import java.util.Random;

/**
 * ChunkGenerator com terreno orgânico — montanhas, vales e colinas.
 * Implementa Perlin noise manualmente (API foi removida no Paper novo).
 */
public class HGChunkGenerator extends org.bukkit.generator.ChunkGenerator {

    private static final int BASE_HEIGHT = 64;
    private static final int OCTAVES = 6;

    // Noise grids — 256x256 por octave com seeds diferentes
    private final double[][][] noiseGrids = new double[OCTAVES][256][256];

    public HGChunkGenerator(long worldSeed) {
        Random rng = new Random(worldSeed);
        for (int o = 0; o < OCTAVES; o++) {
            double[][] grid = noiseGrids[o];
            // Preenche grid com valores randomicos
            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 256; j++) {
                    grid[i][j] = rng.nextDouble() * 2 - 1; // -1 a 1
                }
            }
        }
    }

    public HGChunkGenerator() {
        this(System.nanoTime());
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);

        int minY = world.getMinHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (chunkX << 4) + x;
                int worldZ = (chunkZ << 4) + z;

                int height = getHeightAt(worldX, worldZ);
                height = Math.max(minY + 1, Math.min(world.getMaxHeight() - 2, height));

                for (int y = minY; y < height; y++) {
                    if (y < height - 4) {
                        chunkData.setBlock(x, y, z, Material.STONE);
                    } else if (y < height - 1) {
                        chunkData.setBlock(x, y, z, Material.DIRT);
                    } else {
                        chunkData.setBlock(x, y, z, Material.GRASS_BLOCK);
                    }
                }
            }
        }

        return chunkData;
    }

    private int getHeightAt(int x, int z) {
        double total = 0;
        double amplitude = 1.0;
        double frequency = 0.015;
        double maxVal = 0;

        for (int o = 0; o < OCTAVES; o++) {
            double freq = frequency * Math.pow(2, o);
            double val = interpolatedNoise(x * freq, z * freq, o);
            total += val * amplitude;
            maxVal += amplitude;
            amplitude *= 0.5;
        }

        return BASE_HEIGHT + (int) (total / maxVal * 40);
    }

    /** 2D noise com interpolação linear suave usando grid pré-computado. */
    private double interpolatedNoise(double x, double z, int octave) {
        int x0 = ((int) Math.floor(x) % 256 + 256) % 256;
        int z0 = ((int) Math.floor(z) % 256 + 256) % 256;
        int x1 = (x0 + 1) % 256;
        int z1 = (z0 + 1) % 256;

        double dx = x - Math.floor(x);
        double dz = z - Math.floor(z);

        double[][] g = noiseGrids[octave];

        // Interpolação suave (cubic-like)
        double sx = dx * dx * (3 - 2 * dx);
        double sz = dz * dz * (3 - 2 * dz);

        double top = lerp(g[x0][z0], g[x1][z0], sx);
        double bottom = lerp(g[x0][z1], g[x1][z1], sx);

        return lerp(top, bottom, sz);
    }

    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    @Override
    public boolean shouldGenerateCaves() {
        return true; // habilita carvers vanilla: cavernas + ravinas
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return true; // coloca bedrock no fundo (-64)
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
    public boolean shouldGenerateStructures() {
        return true;
    }

    @Override
    public boolean canSpawn(World world, int x, int z) {
        int borderLimit = 256 / 16 + 2;
        return Math.abs(x) <= borderLimit && Math.abs(z) <= borderLimit;
    }
}
