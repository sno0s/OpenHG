package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;
import java.util.Set;

public final class MushroomPopulator extends BlockPopulator {
    private static final Set<Material> SOIL = Set.of(Material.GRASS_BLOCK, Material.DIRT, Material.MYCELIUM, Material.PODZOL);
    private final int density;

    public MushroomPopulator(int density) { this.density = Math.clamp(density, 0, 256); }

    @Override
    public void populate(WorldInfo world, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        if (density == 0) return;
        int tries = density + random.nextInt(11);
        for (int i = 0; i < tries; i++) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            int y = SurfaceBlocks.groundY(region, x, z, world.getMinHeight());
            if (y + 1 >= world.getMaxHeight()) continue;
            if (SOIL.contains(region.getType(x, y, z)) && region.getType(x, y + 1, z).isAir()) {
                region.setType(x, y + 1, z, random.nextBoolean() ? Material.BROWN_MUSHROOM : Material.RED_MUSHROOM);
            }
        }
    }
}
