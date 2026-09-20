package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.List;

public final class HGWorldProvider extends BiomeProvider {
    private final TerrainProfile terrain;

    public HGWorldProvider(TerrainProfile terrain) { this.terrain = terrain; }

    @Override
    public Biome getBiome(WorldInfo world, int x, int y, int z) {
        return switch (terrain.biomeAt(world.getSeed(), x, z)) {
            case PLAINS -> Biome.PLAINS;
            case FOREST -> Biome.FOREST;
            case BIRCH_FOREST -> Biome.BIRCH_FOREST;
            case DARK_FOREST -> Biome.DARK_FOREST;
            case JUNGLE -> Biome.JUNGLE;
        };
    }

    @Override
    public List<Biome> getBiomes(WorldInfo world) {
        return List.of(Biome.PLAINS, Biome.FOREST, Biome.BIRCH_FOREST, Biome.DARK_FOREST, Biome.JUNGLE);
    }
}
