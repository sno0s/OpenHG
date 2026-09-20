package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.RegionAccessor;

/** Finds ground below trees and vegetation instead of treating the canopy as soil. */
final class SurfaceBlocks {
    private SurfaceBlocks() {}

    static boolean isCanopy(Material material) {
        String name = material.name();
        return name.endsWith("_LEAVES") || name.endsWith("_LOG") || name.endsWith("_WOOD")
                || material == Material.MUSHROOM_STEM || material == Material.BROWN_MUSHROOM_BLOCK
                || material == Material.RED_MUSHROOM_BLOCK;
    }

    static int groundY(RegionAccessor region, int x, int z, int minY) {
        int y = region.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
        while (y > minY) {
            Material type = region.getType(x, y, z);
            if (!type.isAir() && !isCanopy(type) && !TrashCleanPopulator.CLEAR_BLOCKS.contains(type)
                    && (type.isSolid() || type == Material.WATER || type == Material.LAVA)) return y;
            y--;
        }
        return minY;
    }
}
