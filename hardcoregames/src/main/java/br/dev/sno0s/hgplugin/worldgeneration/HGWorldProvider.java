package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Biome provider baseado em dois canais de noise (temperatura e umidade).
 * Cada canal usa value noise interpolado em baixa frequência, gerando
 * regiões contínuas de ~200–300 blocos — semelhante ao vanilla.
 *
 * Ajuste FREQUENCY para mudar o tamanho dos biomas:
 *   menor → biomas maiores | maior → biomas menores
 */
public class HGWorldProvider extends BiomeProvider {

    private static final List<Biome> ALLOWED = List.of(
            Biome.PLAINS,
            Biome.BIRCH_FOREST,
            Biome.FOREST,
            Biome.DARK_FOREST
    );

    // Seeds derivadas para os dois canais (temperatura e umidade)
    private static final long TEMP_SEED  = 0x9E3779B97F4A7C15L;
    private static final long HUMID_SEED = 0x6C62272E07BB0142L;

    // Lidos do config uma única vez na construção
    private final double frequency;
    private final double plainsWeight;
    private final double darkForestWeight;

    public HGWorldProvider() {
        this.frequency       = Hgplugin.getConfigManager().getBiomeFrequency();
        this.plainsWeight    = Hgplugin.getConfigManager().getBiomePlainsWeight();
        this.darkForestWeight = Hgplugin.getConfigManager().getBiomeDarkForestWeight();
    }

    @NotNull
    @Override
    public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        long seed = worldInfo.getSeed();

        double temp  = smoothNoise(x * frequency, z * frequency, seed ^ TEMP_SEED);
        double humid = smoothNoise(x * frequency, z * frequency, seed ^ HUMID_SEED);

        // PLAINS domina quando umidade for baixa (configurável via plains-weight)
        if (humid < plainsWeight) return Biome.PLAINS;
        if (temp >= 0)            return Biome.FOREST;
        return humid > darkForestWeight ? Biome.BIRCH_FOREST : Biome.DARK_FOREST;
    }

    /**
     * Value noise 2D com interpolação suavizada (smoothstep).
     * Não usa grids — calcula hash por vértice da célula.
     */
    private double smoothNoise(double x, double z, long seed) {
        long x0 = (long) Math.floor(x);
        long z0 = (long) Math.floor(z);

        double dx = x - x0;
        double dz = z - z0;

        // Smoothstep: 3t² - 2t³
        double sx = dx * dx * (3 - 2 * dx);
        double sz = dz * dz * (3 - 2 * dz);

        double n00 = hash(x0,     z0,     seed);
        double n10 = hash(x0 + 1, z0,     seed);
        double n01 = hash(x0,     z0 + 1, seed);
        double n11 = hash(x0 + 1, z0 + 1, seed);

        return lerp(lerp(n00, n10, sx), lerp(n01, n11, sx), sz);
    }

    /** Hash determinístico de dois longs → double em [-1, 1]. */
    private double hash(long x, long z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0x6C62272E07BB0142L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        // Mapeia para [-1, 1]
        return (double) (h & Long.MAX_VALUE) / Long.MAX_VALUE * 2.0 - 1.0;
    }

    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    @NotNull
    @Override
    public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return ALLOWED;
    }
}
