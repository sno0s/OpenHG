package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RavineLayoutTest {
    @Test
    void everySeedHasThreeRavinesInsideTheArenaAwayFromSpawn() {
        for (int size : new int[]{256, 500, 750, 1200, 10000}) {
            for (long seed : new long[]{0, 1, -1, 42, 987654321, Long.MIN_VALUE, Long.MAX_VALUE}) {
                RavineLayout layout = new RavineLayout(seed, size);
                assertEquals(layout.ravines(), new RavineLayout(seed, size).ravines());
                assertEquals(3, layout.ravines().size());
                for (RavineLayout.Ravine ravine : layout.ravines()) {
                    assertTrue(ravine.depthAt((int) Math.round(ravine.x()), (int) Math.round(ravine.z())) >= 20);
                    double reach = ravine.halfLength() + ravine.halfWidth() + ravine.bend();
                    assertTrue(Math.abs(ravine.x()) + reach < size / 2.0 - 8);
                    assertTrue(Math.abs(ravine.z()) + reach < size / 2.0 - 8);
                    assertTrue(Math.hypot(ravine.x(), ravine.z()) - reach > 34);
                }
                assertEquals(0, layout.depthAt(0, 0));
            }
        }
        assertNotEquals(new RavineLayout(1, 750).ravines(), new RavineLayout(2, 750).ravines());
    }

    @Test
    void generatorCarvesTheSameWorldCoordinatesAcrossChunkBoundariesAndPreservesBedrock() {
        TerrainProfile terrain = TerrainProfile.classic();
        HGChunkGenerator generator = new HGChunkGenerator(terrain, 0, .25);
        WorldInfo world = mock(WorldInfo.class);
        when(world.getSeed()).thenReturn(42L);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        RavineLayout layout = new RavineLayout(42, 750);
        int carved = 0;
        for (int cx = -20; cx <= 20; cx++) {
            for (int cz = -20; cz <= 20; cz++) {
                ChunkData data = mock(ChunkData.class);
                when(data.getMinHeight()).thenReturn(-64);
                generator.generateCaves(world, new Random(cx), cx, cz, data);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int wx = (cx << 4) + x, wz = (cz << 4) + z;
                        int depth = layout.depthAt(wx, wz);
                        if (depth == 0) continue;
                        int top = terrain.heightAt(42, wx, wz);
                        verify(data).setRegion(x, Math.max(-63, top - depth + 1), z,
                                x + 1, top + 1, z + 1, Material.AIR);
                        carved++;
                    }
                }
            }
        }
        assertTrue(carved > 1000);
    }
}
