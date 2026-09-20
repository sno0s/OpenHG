package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.listeners.ChunkDecorationListener;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PopulatorsTest {
    private ServerMock server;
    private WorldMock world;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("hg_world");
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Chunk prepareChunk(int chunkX, int chunkZ, Biome biome, Material soil) {
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk.getBlock(x, 68, z).setType(soil, false);
                for (int y = 68; y <= 80; y++) {
                    world.setBiome((chunkX << 4) + x, y, (chunkZ << 4) + z, biome);
                }
            }
        }
        return chunk;
    }

    private int count(Chunk chunk, Material material, int minY, int maxY) {
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (chunk.getBlock(x, y, z).getType() == material) count++;
                }
            }
        }
        return count;
    }

    @Test
    void openPlainsAndForestsReceiveTheConfiguredAmountAfterCleanup() {
        Biome[] biomes = {Biome.PLAINS, Biome.FOREST, Biome.BIRCH_FOREST, Biome.DARK_FOREST};
        for (int i = 0; i < biomes.length; i++) {
            Chunk chunk = prepareChunk(i - 2, -1, biomes[i], Material.GRASS_BLOCK);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.getBlock(x, 69, z).setType(x % 2 == 0 ? Material.LEAF_LITTER : Material.SHORT_GRASS, false);
                    if (i > 0) chunk.getBlock(x, 75, z).setType(Material.OAK_LEAVES, false);
                }
            }
            new ChunkDecorationListener(plugin, 40, 0).decorateChunk(chunk);
            int brown = count(chunk, Material.BROWN_MUSHROOM, 69, 69);
            int red = count(chunk, Material.RED_MUSHROOM, 69, 69);
            assertTrue(brown + red >= 40 && brown + red <= 50, "Missing mushrooms in " + biomes[i]);
            assertTrue(Math.abs(brown - red) <= 1);
            assertEquals(0, count(chunk, Material.LEAF_LITTER, 69, 69));
            assertEquals(0, count(chunk, Material.SHORT_GRASS, 69, 69));
            if (i > 0) assertEquals(256, count(chunk, Material.OAK_LEAVES, 75, 75));
        }
    }

    @Test
    void scheduledDecorationRunsOnceAndDoesNotReplenishHarvestedResources() {
        Chunk chunk = prepareChunk(0, 0, Biome.PLAINS, Material.GRASS_BLOCK);
        ChunkDecorationListener listener = new ChunkDecorationListener(plugin, 40, 0);
        server.getPluginManager().registerEvents(listener, plugin);
        server.getPluginManager().callEvent(new ChunkLoadEvent(chunk, true));
        server.getPluginManager().callEvent(new ChunkPopulateEvent(chunk));
        assertEquals(0, count(chunk, Material.RED_MUSHROOM, 69, 69));
        server.getScheduler().performTicks(2);
        int original = count(chunk, Material.RED_MUSHROOM, 69, 69) + count(chunk, Material.BROWN_MUSHROOM, 69, 69);
        assertTrue(original >= 40 && original <= 50);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) chunk.getBlock(x, 69, z).setType(Material.AIR, false);
        }
        chunk.getBlock(3, 69, 3).setType(Material.LEAF_LITTER, false);
        server.getPluginManager().callEvent(new ChunkLoadEvent(chunk, false));
        server.getScheduler().performTicks(2);
        assertEquals(0, count(chunk, Material.RED_MUSHROOM, 69, 69));
        assertEquals(0, count(chunk, Material.BROWN_MUSHROOM, 69, 69));
        assertEquals(0, count(chunk, Material.LEAF_LITTER, 69, 69));
    }

    @Test
    void jungleHasNoMushroomsAndDesertSandDoesNotReceiveThem() {
        for (Biome biome : new Biome[]{Biome.JUNGLE, Biome.SPARSE_JUNGLE, Biome.BAMBOO_JUNGLE}) {
            Chunk chunk = prepareChunk(0, 0, biome, Material.GRASS_BLOCK);
            chunk.getBlock(1, 69, 1).setType(Material.BROWN_MUSHROOM, false);
            chunk.getBlock(2, 69, 2).setType(Material.RED_MUSHROOM, false);
            TrashCleanPopulator.cleanChunk(chunk);
            new MushroomPopulator(256).populate(world, new Random(42), chunk);
            assertEquals(0, count(chunk, Material.BROWN_MUSHROOM, 69, 69));
            assertEquals(0, count(chunk, Material.RED_MUSHROOM, 69, 69));
        }
        Chunk desert = prepareChunk(1, 0, Biome.DESERT, Material.SAND);
        new MushroomPopulator(40).populate(world, new Random(42), desert);
        assertEquals(0, count(desert, Material.BROWN_MUSHROOM, 69, 69));
        assertEquals(0, count(desert, Material.RED_MUSHROOM, 69, 69));
    }

    @Test
    void mixedChunksRespectBiomeBordersAndDenseChunksUseAllAvailableSoil() {
        Chunk chunk = prepareChunk(-1, 0, Biome.FOREST, Material.GRASS_BLOCK);
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 68; y <= 80; y++) world.setBiome(-16 + x, y, z, Biome.JUNGLE);
            }
        }
        new MushroomPopulator(256).populate(world, new Random(42), chunk);
        assertEquals(128, count(chunk, Material.BROWN_MUSHROOM, 69, 69) + count(chunk, Material.RED_MUSHROOM, 69, 69));
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 16; z++) assertEquals(Material.AIR, chunk.getBlock(x, 69, z).getType());
        }
    }

    @Test
    void cocoaAttachesToJungleLogsWithinTheConfiguredLimit() {
        Chunk chunk = prepareChunk(0, 0, Biome.JUNGLE, Material.GRASS_BLOCK);
        for (int y = 69; y <= 73; y++) chunk.getBlock(8, y, 8).setType(Material.JUNGLE_LOG, false);
        new CocoaPopulator(12).populate(world, new Random(42), chunk);
        assertEquals(12, count(chunk, Material.COCOA, 69, 73));
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 69; y <= 73; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getType() != Material.COCOA) continue;
                    Cocoa cocoa = (Cocoa) block.getBlockData();
                    assertEquals(Material.JUNGLE_LOG, block.getRelative(cocoa.getFacing()).getType());
                    assertEquals(cocoa.getMaximumAge(), cocoa.getAge());
                }
            }
        }
        Chunk disabled = prepareChunk(1, 0, Biome.JUNGLE, Material.GRASS_BLOCK);
        disabled.getBlock(8, 69, 8).setType(Material.JUNGLE_LOG, false);
        new CocoaPopulator(0).populate(world, new Random(42), disabled);
        assertEquals(0, count(disabled, Material.COCOA, 69, 73));
        Chunk forest = prepareChunk(2, 0, Biome.FOREST, Material.GRASS_BLOCK);
        forest.getBlock(8, 69, 8).setType(Material.JUNGLE_LOG, false);
        new CocoaPopulator(12).populate(world, new Random(42), forest);
        assertEquals(0, count(forest, Material.COCOA, 69, 73));
    }

    @Test
    void mobLimitsAndGenerationUseQuarterPopulation() {
        World target = mock(World.class);
        when(target.getSpawnLimit(any())).thenReturn(20);
        MobPopulation.configure(target, .25);
        for (SpawnCategory category : SpawnCategory.values()) {
            if (category != SpawnCategory.MISC) verify(target).setSpawnLimit(category, 5);
        }
        verify(target, never()).setSpawnLimit(eq(SpawnCategory.MISC), anyInt());
        HGChunkGenerator generator = new HGChunkGenerator(TerrainProfile.classic(), 0, .25);
        Random random = new Random(42);
        int allowed = 0;
        for (int i = 0; i < 10000; i++) if (generator.shouldGenerateMobs(target, random, i, 0)) allowed++;
        assertTrue(allowed > 2350 && allowed < 2650);
        assertFalse(new HGChunkGenerator(TerrainProfile.classic(), 0, 0).shouldGenerateMobs(target, random, 0, 0));
        assertTrue(new HGChunkGenerator(TerrainProfile.classic(), 0, 1).shouldGenerateMobs(target, random, 0, 0));
    }
}
