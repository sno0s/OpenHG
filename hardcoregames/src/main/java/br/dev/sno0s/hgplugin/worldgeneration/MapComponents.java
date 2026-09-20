package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;

public final class MapComponents {
    private MapComponents() {}

    public static void gerarParede(JavaPlugin plugin, World world, int size, TerrainProfile terrain, int height) {
        WallLayout layout = new WallLayout(terrain, world.getSeed(), size, height);
        Iterator<WallLayout.Column> columns = layout.columns().iterator();
        // The border protects the arena while the physical wall is still being built.
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(size);
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            // Towers use the same per-tick budget as the walls.
            for (int i = 0; i < 8 && columns.hasNext(); i++) {
                buildColumn(world, terrain, columns.next());
            }
            if (!columns.hasNext()) {
                task.cancel();
                plugin.getLogger().info(Messages.log("console.map-components.wall-finished", "size", size, "height", layout.wallTop()));
            }
        }, 0L, 1L);
    }

    private static void buildColumn(World world, TerrainProfile terrain, WallLayout.Column column) {
        int x = column.x(), z = column.z(), top = column.top();
        int ground = terrain.heightAt(world.getSeed(), x, z);
        int bottom = column.roofOnly() ? top
                : column.deepFoundation() ? world.getMinHeight() : Math.max(world.getMinHeight(), ground - 3);
        for (int y = bottom; y <= top; y++) {
            Material material;
            if (y < ground - 3) material = Material.BEDROCK; // hidden tunnel barrier
            else if (y <= ground + 1) material = Material.COBBLESTONE;
            else if (y == top) material = Material.SMOOTH_STONE;
            else if (y == top - 3) material = Material.POLISHED_ANDESITE;
            else if (column.accent() && y == top - 1) material = Material.CHISELED_STONE_BRICKS;
            else material = wallMaterial(x, y, z, ground);
            world.getBlockAt(x, y, z).setType(material, false);
        }
        if (column.merlon()) {
            world.getBlockAt(x, top + 1, z).setType(Material.STONE_BRICKS, false);
            world.getBlockAt(x, top + 2, z).setType(Material.STONE_BRICK_SLAB, false);
        } else if (!column.roofOnly()) {
            world.getBlockAt(x, top + 1, z).setType(Material.STONE_BRICK_SLAB, false);
        }
    }

    private static Material wallMaterial(int x, int y, int z, int ground) {
        int value = Math.floorMod((x * 73856093) ^ (y * 19349663) ^ (z * 83492791), 100);
        if (value < 7) return Material.CRACKED_STONE_BRICKS;
        if (y < ground + 6 && value < 22) return Material.MOSSY_STONE_BRICKS;
        return Material.STONE_BRICKS;
    }
}
