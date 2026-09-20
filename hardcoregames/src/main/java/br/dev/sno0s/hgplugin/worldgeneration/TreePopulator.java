package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/** Supplements vanilla forests without accessing live chunks during generation. */
public final class TreePopulator extends BlockPopulator {
    private final int density;

    public TreePopulator(int density) { this.density = Math.clamp(density, 0, 8); }

    @Override
    public void populate(WorldInfo world, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        for (int i = 0; i < 12 * density; i++) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            if (x * (long) x + z * (long) z < 14 * 14) continue;
            int y = SurfaceBlocks.groundY(region, x, z, world.getMinHeight());
            if (y + 32 >= world.getMaxHeight() || region.getType(x, y, z) != Material.GRASS_BLOCK) continue;
            Biome biome = region.getBiome(x, y, z);
            double chance = biome == Biome.PLAINS ? 0.035 : biome == Biome.DARK_FOREST ? 0.8 : 0.55;
            if (random.nextDouble() >= chance) continue;
            TreeType type = biome == Biome.BIRCH_FOREST ? TreeType.BIRCH
                    : biome == Biome.DARK_FOREST ? TreeType.DARK_OAK
                    : biome == Biome.JUNGLE ? (random.nextInt(4) == 0 ? TreeType.JUNGLE : TreeType.SMALL_JUNGLE)
                    : random.nextInt(5) == 0 ? TreeType.BIRCH : TreeType.TREE;
            region.generateTree(new Location(region.getWorld(), x, y + 1, z), random, type);
        }
    }
}
