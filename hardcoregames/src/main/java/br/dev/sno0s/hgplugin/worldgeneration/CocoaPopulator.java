package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.generator.BlockPopulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CocoaPopulator extends BlockPopulator {

    private static final BlockFace[] SIDES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };
    private final int density;

    public CocoaPopulator(int density) {
        this.density = Math.clamp(density, 0, 256);
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        if (density == 0) return;

        List<Block> logs = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (chunk.getX() << 4) + x;
                int worldZ = (chunk.getZ() << 4) + z;
                int ground = SurfaceBlocks.groundY(world, worldX, worldZ, world.getMinHeight());
                if (!MushroomPopulator.isJungle(world.getBiome(worldX, ground, worldZ))) continue;

                // Cacau ao alcance dos jogadores, preso aos troncos naturais da jungle.
                int top = Math.min(ground + 5, world.getMaxHeight() - 1);
                for (int y = ground + 1; y <= top; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getType() == Material.JUNGLE_LOG || block.getType() == Material.JUNGLE_WOOD) {
                        logs.add(block);
                    }
                }
            }
        }

        Collections.shuffle(logs, random);
        int placed = 0;
        for (Block log : logs) {
            int firstSide = random.nextInt(SIDES.length);
            for (int i = 0; i < SIDES.length; i++) {
                BlockFace side = SIDES[(firstSide + i) % SIDES.length];
                int x = (log.getX() & 15) + side.getModX();
                int z = (log.getZ() & 15) + side.getModZ();
                if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;

                Block block = chunk.getBlock(x, log.getY(), z);
                if (!block.getType().isAir() || !MushroomPopulator.isJungle(block.getBiome())) continue;

                Cocoa cocoa = (Cocoa) Material.COCOA.createBlockData();
                cocoa.setFacing(side.getOppositeFace());
                cocoa.setAge(cocoa.getMaximumAge());
                block.setBlockData(cocoa, false);
                placed++;
                if (placed >= density) return;
            }
        }
    }
}
