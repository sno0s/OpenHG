package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorldResetTest {
    @TempDir Path temporary;
    private final Logger logger = Logger.getLogger("WorldResetTest");

    @Test
    void resetsUnloadedPaper26DimensionAndPreservesMainWorldAndOtherDimensions() throws IOException {
        Fixture f = new Fixture("world/dimensions/minecraft/hg_world", false);
        Path player = f.container.resolve("world/players/data/player.dat");
        Path overworld = f.container.resolve("world/dimensions/minecraft/overworld/region/main.mca");
        Path nether = f.container.resolve("world/dimensions/minecraft/the_nether/region/nether.mca");
        for (Path file : List.of(player, overworld, nether)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "preserve");
        }

        assertSame(f.fresh, WorldReset.recreate(f.server, f.creator, logger));
        assertFalse(Files.exists(f.marker));
        assertEquals(1234L, f.fresh.getSeed());
        for (Path file : List.of(player, overworld, nether)) assertEquals("preserve", Files.readString(file));
        verify(f.server, times(2)).createWorld(f.creator);
        verify(f.server).unloadWorld(f.previous, false);
        assertFalse(Files.exists(f.container.resolve("hg_world")));
    }

    @Test
    void resetsAlreadyLoadedLegacyWorld() throws IOException {
        Fixture f = new Fixture("hg_world", true);
        assertSame(f.fresh, WorldReset.recreate(f.server, f.creator, logger));
        assertFalse(Files.exists(f.marker));
        verify(f.server, times(1)).createWorld(f.creator);
    }

    @Test
    void cancelledUnloadPreservesAllOldFiles() throws IOException {
        Fixture f = new Fixture("world/dimensions/minecraft/hg_world", true);
        doReturn(false).when(f.server).unloadWorld(f.previous, false);
        assertThrows(IllegalStateException.class, () -> WorldReset.recreate(f.server, f.creator, logger));
        assertTrue(Files.exists(f.marker));
        verify(f.server, never()).createWorld(any());
    }

    @Test
    void neverDeletesThePrimaryWorldEvenIfItIsNamedHgWorld() throws IOException {
        Fixture f = new Fixture("hg_world", true);
        when(f.server.getWorlds()).thenReturn(List.of(f.previous));
        assertThrows(IllegalStateException.class, () -> WorldReset.recreate(f.server, f.creator, logger));
        assertTrue(Files.exists(f.marker));
        verify(f.server, never()).unloadWorld(any(World.class), anyBoolean());
    }

    @Test
    void rejectsParentDirectoryReportedAsArenaFolder() throws IOException {
        Fixture f = new Fixture("world/dimensions/minecraft/hg_world", true);
        when(f.previous.getWorldFolder()).thenReturn(f.container.resolve("world").toFile());
        assertThrows(IOException.class, () -> WorldReset.recreate(f.server, f.creator, logger));
        assertTrue(Files.exists(f.marker));
        verify(f.server, never()).unloadWorld(any(World.class), anyBoolean());
    }

    @Test
    void rejectsFoldersOutsideTheServerContainer() throws IOException {
        Fixture f = new Fixture("hg_world", true);
        Path outside = temporary.resolve("outside/hg_world");
        Files.createDirectories(outside);
        Path marker = Files.writeString(outside.resolve("keep.txt"), "preserve");
        when(f.previous.getWorldFolder()).thenReturn(outside.toFile());
        assertThrows(IOException.class, () -> WorldReset.recreate(f.server, f.creator, logger));
        assertTrue(Files.exists(marker));
    }

    @Test
    void rejectsArenaFolderContainingAnotherLoadedWorld() throws IOException {
        Fixture f = new Fixture("hg_world", true);
        World nested = mock(World.class);
        when(nested.getWorldFolder()).thenReturn(f.folder.resolve("other_world").toFile());
        when(f.server.getWorlds()).thenReturn(List.of(f.primary, f.previous, nested));
        assertThrows(IOException.class, () -> WorldReset.recreate(f.server, f.creator, logger));
        assertTrue(Files.exists(f.marker));
    }

    @Test
    void refusesToSilentlyReusePersistedSeed() throws IOException {
        Fixture f = new Fixture("world/dimensions/minecraft/hg_world", true);
        when(f.fresh.getSeed()).thenReturn(42L);
        assertThrows(IllegalStateException.class, () -> WorldReset.recreate(f.server, f.creator, logger));
    }

    @Test
    void guaranteesSeedChangesEvenIfRandomSeedMatchesOldSeed() throws IOException {
        Fixture f = new Fixture("hg_world", true);
        f.seed.set(42L);
        WorldReset.recreate(f.server, f.creator, logger);
        assertNotEquals(42L, f.fresh.getSeed());
    }

    private final class Fixture {
        final Server server = mock(Server.class);
        final WorldCreator creator = mock(WorldCreator.class);
        final World primary = mock(World.class), previous = mock(World.class), fresh = mock(World.class);
        final AtomicLong seed = new AtomicLong(1234);
        final AtomicReference<World> loaded;
        final Path container, folder, marker;

        Fixture(String arenaPath, boolean initiallyLoaded) throws IOException {
            container = Files.createDirectories(temporary.resolve("server"));
            folder = Files.createDirectories(container.resolve(arenaPath));
            Path region = Files.createDirectories(folder.resolve("region"));
            marker = Files.writeString(region.resolve("old-chunk.mca"), "old arena data");
            Files.createDirectories(container.resolve("world"));
            loaded = new AtomicReference<>(initiallyLoaded ? previous : null);
            when(server.getWorldContainer()).thenReturn(container.toFile());
            when(server.getWorlds()).thenReturn(List.of(primary, previous));
            when(primary.getWorldFolder()).thenReturn(container.resolve("world").toFile());
            when(primary.getName()).thenReturn("world");
            when(previous.getName()).thenReturn("hg_world");
            when(previous.getWorldFolder()).thenReturn(folder.toFile());
            when(previous.getSeed()).thenReturn(42L);
            when(fresh.getWorldFolder()).thenReturn(folder.toFile());
            when(fresh.getSeed()).thenAnswer(call -> seed.get());
            when(creator.name()).thenReturn("hg_world");
            when(creator.seed()).thenAnswer(call -> seed.get());
            when(creator.seed(anyLong())).thenAnswer(call -> {
                seed.set(call.getArgument(0));
                return creator;
            });
            when(server.getWorld("hg_world")).thenAnswer(call -> loaded.get());
            when(server.unloadWorld(previous, false)).thenAnswer(call -> {
                loaded.set(null);
                return true;
            });
            when(server.createWorld(creator)).thenAnswer(call -> {
                World result = Files.exists(marker) ? previous : fresh;
                Files.createDirectories(folder);
                loaded.set(result);
                return result;
            });
        }
    }
}
