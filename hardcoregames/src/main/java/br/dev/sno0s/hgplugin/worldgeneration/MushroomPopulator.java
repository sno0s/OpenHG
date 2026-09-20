package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Material;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MushroomPopulator extends BlockPopulator {
    private static final Set<Material> ALLOWED_BLOCKS = Set.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.MYCELIUM, Material.PODZOL);
    private final int density;

    public MushroomPopulator(int density) {
        this.density = Math.clamp(density, 0, 256);
    }

    // Chamado pelo listener no próximo tick, com o chunk pronto, como no populator original.
    @Override
    public void populate(World world, Random random, Chunk chunk) {
        if (density == 0) return;

        int target = Math.min(256, density + random.nextInt(11));
        int existing = 0;
        List<Block> available = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (chunk.getX() << 4) + x;
                int worldZ = (chunk.getZ() << 4) + z;
                int y = SurfaceBlocks.groundY(world, worldX, worldZ, world.getMinHeight());
                if (y + 1 >= world.getMaxHeight()) continue;
                if (isJungle(world.getBiome(worldX, y, worldZ))) continue;

                Block ground = chunk.getBlock(x, y, z);
                Block above = chunk.getBlock(x, y + 1, z);
                if (!ALLOWED_BLOCKS.contains(ground.getType())) continue;

                if (isMushroom(above.getType())) {
                    existing++;
                } else if (above.getType().isAir()) {
                    available.add(above);
                }
            }
        }

        Collections.shuffle(available, random);
        int amount = Math.min(Math.max(0, target - existing), available.size());
        boolean brown = random.nextBoolean();
        for (int i = 0; i < amount; i++) {
            // Sem física: o Minecraft remove cogumelos expostos à luz ao atualizar vizinhos.
            available.get(i).setType(brown ? Material.BROWN_MUSHROOM : Material.RED_MUSHROOM, false);
            brown = !brown;
        }
    }

    public static boolean isMushroom(Material material) {
        return material == Material.BROWN_MUSHROOM || material == Material.RED_MUSHROOM;
    }

    public static boolean isJungle(Biome biome) {
        return biome == Biome.JUNGLE || biome == Biome.SPARSE_JUNGLE || biome == Biome.BAMBOO_JUNGLE;
    }
}
