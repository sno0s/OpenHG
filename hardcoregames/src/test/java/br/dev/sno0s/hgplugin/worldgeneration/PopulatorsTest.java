package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PopulatorsTest {
    @BeforeEach void setUp() { MockBukkit.mock(); }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    @Test
    void cleansUnderCanopyBeforePlantingMushroomsOnSoil() {
        LimitedRegion region = mock(LimitedRegion.class);
        WorldInfo world = mock(WorldInfo.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(region.getHighestBlockYAt(anyInt(), anyInt(), eq(HeightMap.WORLD_SURFACE))).thenReturn(75);
        boolean[][] cleaned = new boolean[16][16];
        Material[][] mushrooms = new Material[16][16];
        when(region.getType(anyInt(), anyInt(), anyInt())).thenAnswer(call -> {
            int x = call.getArgument(0), y = call.getArgument(1), z = call.getArgument(2);
            if (y == 75) return Material.OAK_LEAVES;
            if (y == 68) return Material.GRASS_BLOCK;
            if (y == 69) {
                if (mushrooms[x][z] != null) return mushrooms[x][z];
                return cleaned[x][z] ? Material.AIR : Material.SHORT_GRASS;
            }
            return Material.AIR;
        });
        doAnswer(call -> {
            int x = call.getArgument(0), y = call.getArgument(1), z = call.getArgument(2);
            Material type = call.getArgument(3);
            assertEquals(69, y, "Only the vegetation layer should change");
            if (type == Material.AIR) cleaned[x][z] = true;
            else mushrooms[x][z] = type;
            return null;
        }).when(region).setType(anyInt(), anyInt(), anyInt(), any(Material.class));
        var populators = new HGChunkGenerator(TerrainProfile.classic(), 0, 40).getDefaultPopulators(null);
        for (var populator : populators) populator.populate(world, new Random(42), 0, 0, region);
        int brown = 0, red = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertTrue(cleaned[x][z]);
                if (mushrooms[x][z] == Material.BROWN_MUSHROOM) brown++;
                if (mushrooms[x][z] == Material.RED_MUSHROOM) red++;
            }
        }
        assertTrue(brown >= 10 && red >= 10, "Both soup ingredients must grow below trees");
        new TrashCleanPopulator().populate(world, new Random(42), 0, 0, region);
        verify(region, never()).setType(anyInt(), eq(75), anyInt(), any());
    }

    @Test
    void mobLimitsAndGenerationUseQuarterPopulation() {
        World world = mock(World.class);
        when(world.getSpawnLimit(any())).thenReturn(20);
        MobPopulation.configure(world, .25);
        for (SpawnCategory category : SpawnCategory.values()) {
            if (category != SpawnCategory.MISC) verify(world).setSpawnLimit(category, 5);
        }
        verify(world, never()).setSpawnLimit(eq(SpawnCategory.MISC), anyInt());
        HGChunkGenerator generator = new HGChunkGenerator(TerrainProfile.classic(), 0, 0, .25);
        Random random = new Random(42);
        int allowed = 0;
        for (int i = 0; i < 10000; i++) if (generator.shouldGenerateMobs(world, random, i, 0)) allowed++;
        assertTrue(allowed > 2350 && allowed < 2650);
        assertFalse(new HGChunkGenerator(TerrainProfile.classic(), 0, 0, 0).shouldGenerateMobs(world, random, 0, 0));
        assertTrue(new HGChunkGenerator(TerrainProfile.classic(), 0, 0, 1).shouldGenerateMobs(world, random, 0, 0));
    }
}
