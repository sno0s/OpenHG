package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

import java.util.Random;

public class TreePopulator extends BlockPopulator {

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        // chance de gerar árvore por chunk (configurável, padrão 1-2)
        int tries = Hgplugin.getConfigManager().getConfig().getInt("HGconfigs.tree-density", 0) + random.nextInt(2);

        int chunkX = chunk.getX() << 4;
        int chunkZ = chunk.getZ() << 4;

        for (int i = 0; i < tries; i++) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);
            int y = getTopNonTransparentY(world, x, z);

            // só gera se o topo for grass
            if (y < 0 || world.getBlockAt(x, y, z).getType() != Material.GRASS_BLOCK) continue;

            generateTree(world, x, y + 1, z, random);
        }
    }

    /** Encontra o maior y com bloco não-transparente (o topo do terreno). */
    private int getTopNonTransparentY(World world, int x, int z) {
        for (int y = world.getMaxHeight() - 1; y > 0; y--) {
            Block b = world.getBlockAt(x, y, z);
            if (!b.getType().isAir() && b.getType() != Material.SHORT_GRASS && b.getType() != Material.TALL_GRASS) {
                return y;
            }
        }
        return -1;
    }

    /** Gera uma árvore de 3-6 blocos de altura aleatória. */
    private void generateTree(World world, int x, int y, int z, Random random) {
        int trunkHeight = 3 + random.nextInt(4); // 3-6 blocos

        // tronco
        for (int h = 0; h < trunkHeight; h++) {
            world.getBlockAt(x, y + h, z).setType(Material.OAK_LOG, false);
        }

        // folhas
        int leafStart = y + trunkHeight - 2;
        int leafEnd = y + trunkHeight + 1;
        for (int ly = leafStart; ly <= leafEnd; ly++) {
            for (int lx = -2; lx <= 2; lx++) {
                for (int lz = -2; lz <= 2; lz++) {
                    if (Math.abs(lx) == 2 && Math.abs(lz) == 2 && random.nextBoolean()) continue; // cantos aleatórios
                    Block block = world.getBlockAt(x + lx, ly, z + lz);
                    if (block.getType().isAir()) {
                        block.setType(Material.OAK_LEAVES, false);
                    }
                }
            }
        }
    }
}
