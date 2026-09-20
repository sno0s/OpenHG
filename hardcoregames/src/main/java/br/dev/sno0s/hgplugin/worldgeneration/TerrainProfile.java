package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.utils.Messages;
/** Modelo de terreno compartilhado pelos chunks, biomas e muralha. */
public final class TerrainProfile {
    public enum Landscape { PLAINS, FOREST, DARK_FOREST, JUNGLE, DESERT }
    private static final Landscape[] REQUIRED_BIOMES = {
            Landscape.PLAINS, Landscape.FOREST, Landscape.DARK_FOREST, Landscape.JUNGLE, Landscape.DESERT
    };
    private static final int[][] BIOME_CENTERS = {{0, 220}, {209, 68}, {129, -178}, {-129, -178}, {-209, 68}};
    private static final int[][] MOUNTAIN_CENTERS = {{170, 150}, {-170, -150}, {170, -150}, {-170, 150}};
    private final int worldSize;

    private final int baseHeight;
    private final double variation;
    private final double hillFrequency;
    private final double biomeFrequency;
    private final double plainsThreshold;
    private final double darkForestThreshold;

    public TerrainProfile(int baseHeight, double variation, double hillFrequency,
                          double biomeFrequency, double plainsThreshold, double darkForestThreshold) {
        this(baseHeight, variation, hillFrequency, biomeFrequency, plainsThreshold, darkForestThreshold, 750);
    }

    public TerrainProfile(int baseHeight, double variation, double hillFrequency,
                          double biomeFrequency, double plainsThreshold, double darkForestThreshold, int worldSize) {
        if (baseHeight < 48 || baseHeight > 112
                || !Double.isFinite(variation) || variation < 0 || variation > 24
                || !Double.isFinite(hillFrequency) || hillFrequency < 0.001 || hillFrequency > 0.02
                || !Double.isFinite(biomeFrequency) || biomeFrequency < 0.001 || biomeFrequency > 0.05
                || !Double.isFinite(plainsThreshold) || Math.abs(plainsThreshold) > 1
                || !Double.isFinite(darkForestThreshold) || Math.abs(darkForestThreshold) > 1
                || worldSize < 256 || worldSize > 10000 || worldSize % 2 != 0) {
            throw new IllegalArgumentException(Messages.text("console.terrain-profile.invalid-settings"));
        }
        this.baseHeight = baseHeight;
        this.variation = variation;
        this.hillFrequency = hillFrequency;
        this.biomeFrequency = biomeFrequency;
        this.plainsThreshold = plainsThreshold;
        this.darkForestThreshold = darkForestThreshold;
        this.worldSize = worldSize;
    }

    public static TerrainProfile classic() {
        return new TerrainProfile(68, 12, 0.006, 0.008, -0.15, -0.3);
    }

    public int worldSize() {
        return worldSize;
    }

    /** Altura da superfície, com relevo contínuo entre chunks e biomas. */
    public int heightAt(long seed, int x, int z) {
        double broad = noise(seed, x * hillFrequency, z * hillFrequency);
        double hills = noise(seed ^ 0x632BE59BD9B4E019L, x * hillFrequency * 2, z * hillFrequency * 2);
        double detail = noise(seed ^ 0x94D049BB133111EBL, x * hillFrequency * 4, z * hillFrequency * 4);
        double relief = variation * (broad * 0.70 + hills * 0.25 + detail * 0.05);
        // Quatro elevações separadas, com posições que variam conforme a seed.
        double scale = worldSize / 750.0;
        double mountain = 0;
        for (int i = 0; i < MOUNTAIN_CENTERS.length; i++) {
            double cx = (MOUNTAIN_CENTERS[i][0] + hash(seed, i, 47) * 15) * scale;
            double cz = (MOUNTAIN_CENTERS[i][1] + hash(seed, i, 59) * 15) * scale;
            mountain = Math.max(mountain, mountainAt(x, z, cx, cz, scale));
        }
        relief += variation * 2 * mountain;
        // A clareira central se mistura ao relevo sem formar um degrau circular.
        double blend = smooth(Math.clamp((Math.hypot(x, z) - 10) / 24, 0.0, 1.0));
        return baseHeight + (int) Math.round(relief * blend);
    }

    public Landscape biomeAt(long seed, int x, int z) {
        // Garante áreas abertas, florestas, jungle e deserto dentro da arena.
        double scale = worldSize / 750.0;
        for (int i = 0; i < BIOME_CENTERS.length; i++) {
            double cx = (BIOME_CENTERS[i][0] + hash(seed, i, 17) * 20) * scale;
            double cz = (BIOME_CENTERS[i][1] + hash(seed, i, 31) * 20) * scale;
            double dx = x - cx, dz = z - cz;
            if (dx * dx + dz * dz > 75 * 75 * scale * scale) continue;
            double radius = (65 + 10 * noise(seed ^ i, x * 0.025 / scale, z * 0.025 / scale)) * scale;
            if (dx * dx + dz * dz <= radius * radius) {
                return REQUIRED_BIOMES[(i + (int) Math.floorMod(seed, REQUIRED_BIOMES.length)) % REQUIRED_BIOMES.length];
            }
        }
        double humidity = noise(seed ^ 0x6C62272E07BB0142L, x * biomeFrequency, z * biomeFrequency);
        double temperature = noise(seed ^ 0x9E3779B97F4A7C15L, x * biomeFrequency, z * biomeFrequency);
        if (humidity < plainsThreshold) return Landscape.PLAINS;
        if (temperature > 0.6) return Landscape.DESERT;
        // A floresta de bétulas foi removida; a faixa intermediária usa floresta comum.
        if (temperature < darkForestThreshold) return Landscape.DARK_FOREST;
        if (temperature > 0.5) return Landscape.JUNGLE;
        return Landscape.FOREST;
    }

    private static double mountainAt(int x, int z, double cx, double cz, double scale) {
        return smooth(Math.clamp(1 - Math.hypot(x - cx, z - cz) / (90 * scale), 0.0, 1.0));
    }

    private static double noise(long seed, double x, double z) {
        long ix = (long) Math.floor(x), iz = (long) Math.floor(z);
        double sx = smooth(x - ix), sz = smooth(z - iz);
        return lerp(lerp(hash(seed, ix, iz), hash(seed, ix + 1, iz), sx),
                lerp(hash(seed, ix, iz + 1), hash(seed, ix + 1, iz + 1), sx), sz);
    }

    private static double hash(long seed, long x, long z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0x6C62272E07BB0142L);
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (double) (h & Long.MAX_VALUE) / Long.MAX_VALUE * 2 - 1;
    }

    private static double smooth(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
