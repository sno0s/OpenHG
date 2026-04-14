package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MushroomPopulator extends BlockPopulator {

    // lista de blocos onde os cogumelos podem nascer
    private static final Set<Material> ALLOWED_BLOCKS = new HashSet<>(Arrays.asList(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.MYCELIUM,
            Material.PODZOL
    ));

    @Override
    public void populate(World world, Random random, Chunk chunk) {

        // número de tentativas por chunk (ajuste de acordo com mushroom-density na config.yml)
        int tries = Hgplugin.getConfigManager().getMushroomDensity() + random.nextInt(11); // x a x+10 tentativas

        for (int i = 0; i < tries; i++) {
            int x = (chunk.getX() << 4) + random.nextInt(16);
            int z = (chunk.getZ() << 4) + random.nextInt(16);
            int y = world.getHighestBlockYAt(x, z);

            Block ground = world.getBlockAt(x, y, z); // bloco do chão
            Block above = world.getBlockAt(x, y+1, z);      // onde o cogumelo vai nascer

            // só coloca se o chão for permitido e o espaço estiver livre
            if (ALLOWED_BLOCKS.contains(ground.getType()) && above.getType() == Material.AIR) {
                Material mushroom = random.nextBoolean()
                        ? Material.BROWN_MUSHROOM
                        : Material.RED_MUSHROOM;

                above.setType(mushroom, false);
            }
        }
    }
}
