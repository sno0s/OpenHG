package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.listeners.ArenaStructureListener;
import org.bukkit.Material;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.generator.WorldInfo;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArenaStructuresTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void allowsVillagesAndDesertTemplesOnlyInArena() {
        var arena = server.addSimpleWorld("hg_world");
        var normal = server.addSimpleWorld("world");
        var listener = new ArenaStructureListener();
        for (Structure structure : new Structure[]{Structure.VILLAGE_PLAINS, Structure.VILLAGE_DESERT, Structure.DESERT_PYRAMID}) {
            var event = new AsyncStructureSpawnEvent(arena, structure, new BoundingBox(0, 60, 0, 20, 80, 20), 0, 0);
            listener.onStructureSpawn(event);
            assertFalse(event.isCancelled());
        }
        var blocked = new AsyncStructureSpawnEvent(arena, Structure.MANSION, new BoundingBox(), 0, 0);
        listener.onStructureSpawn(blocked);
        assertTrue(blocked.isCancelled());
        var outside = new AsyncStructureSpawnEvent(normal, Structure.MANSION, new BoundingBox(), 0, 0);
        listener.onStructureSpawn(outside);
        assertFalse(outside.isCancelled());
        assertTrue(new HGChunkGenerator(TerrainProfile.classic(), 0, .25).shouldGenerateStructures());
    }

    @Test
    void desertHasSandSurfaceAndSandstoneSupport() {
        TerrainProfile terrain = TerrainProfile.classic();
        int x = 0, z = 0;
        boolean found = false;
        for (int sampleX = -350; sampleX <= 350 && !found; sampleX += 8) {
            for (int sampleZ = -350; sampleZ <= 350; sampleZ += 8) {
                if (terrain.biomeAt(42, sampleX, sampleZ) == TerrainProfile.Landscape.DESERT) {
                    x = sampleX;
                    z = sampleZ;
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found);
        WorldInfo world = mock(WorldInfo.class);
        when(world.getSeed()).thenReturn(42L);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        ChunkData data = mock(ChunkData.class);
        when(data.getMinHeight()).thenReturn(-64);
        new HGChunkGenerator(terrain, 0, .25).generateNoise(world, new Random(42), x >> 4, z >> 4, data);
        int top = terrain.heightAt(42, x, z);
        verify(data).setBlock(x & 15, top, z & 15, Material.SAND);
        verify(data).setRegion(x & 15, top - 3, z & 15, (x & 15) + 1, top, (z & 15) + 1, Material.SANDSTONE);
    }
}
