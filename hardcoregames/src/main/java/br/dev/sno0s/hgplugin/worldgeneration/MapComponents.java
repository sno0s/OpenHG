package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.TileState;
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
            else material = wallMaterial(x, y, z, ground, top, column.accent());
            place(world, x, y, z, material);
        }
        if (column.merlon()) {
            place(world, x, top + 1, z, column.accent() ? Material.DARK_OAK_PLANKS : Material.STONE_BRICKS);
            place(world, x, top + 2, z, Material.STONE_BRICK_SLAB);
        } else if (!column.roofOnly()) {
            place(world, x, top + 1, z, Material.STONE_BRICK_SLAB);
        }
    }

    private static void place(World world, int x, int y, int z, Material material) {
        var block = world.getBlockAt(x, y, z);
        // Construções anteriores podem deixar baús/fornalhas sob a nova muralha.
        // Limpe o estado de bloco antes de trocar o material para não deixar tile entities órfãs.
        if (block.getState() instanceof TileState) block.setType(Material.AIR, false);
        block.setType(material, false);
    }

    private static Material wallMaterial(int x, int y, int z, int ground, int top, boolean accent) {
        int value = Math.floorMod((x * 73856093) ^ (y * 19349663) ^ (z * 83492791), 100);
        // Contrafortes de madeira marcam a face externa e quebram a massa de pedra.
        if (accent && y >= ground + 2 && y < top - 1) {
            return Math.floorMod(y, 5) == 0 ? Material.DARK_OAK_PLANKS : Material.SPRUCE_LOG;
        }
        // Faixas horizontais de madeira atravessam a muralha em intervalos regulares.
        if (Math.floorMod(y - ground, 12) == 0 && y > ground + 3 && value < 72) {
            return Material.SPRUCE_PLANKS;
        }
        if (value < 7) return Material.CRACKED_STONE_BRICKS;
        if (value < 17) return Material.MOSSY_STONE_BRICKS;
        if (value < 28) return Material.POLISHED_ANDESITE;
        return Material.STONE_BRICKS;
    }
}
