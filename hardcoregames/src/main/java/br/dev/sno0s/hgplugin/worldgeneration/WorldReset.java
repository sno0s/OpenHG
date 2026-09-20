package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Logger;

/** Recreates only the HG arena, using Bukkit's actual storage path on every server version. */
final class WorldReset {
    private static final String WORLD_NAME = "hg_world";

    private WorldReset() {}

    static World recreate(Server server, WorldCreator creator, Logger logger) throws IOException {
        if (!WORLD_NAME.equals(creator.name())) {
            throw new IllegalArgumentException(Messages.text("console.world-reset.wrong-world"));
        }

        World previous = server.getWorld(WORLD_NAME);
        if (previous == null) {
            // Let the server locate/migrate an existing save (including Paper 26 dimensions).
            // Supplying our generator here also prevents any vanilla chunks during discovery.
            previous = server.createWorld(creator);
        }
        if (previous == null) throw new IllegalStateException(Messages.text("console.world-reset.world-not-found"));
        Path folder = validateFolder(server, previous);
        long oldSeed = previous.getSeed();
        if (creator.seed() == oldSeed) creator.seed(oldSeed ^ 0x9E3779B97F4A7C15L);
        long newSeed = creator.seed();

        if (!server.unloadWorld(previous, false) || server.getWorld(WORLD_NAME) != null) {
            throw new IllegalStateException(Messages.text("console.world-reset.unload-failed"));
        }

        logger.info(Messages.log("console.world-reset.recreating", "folder", folder, "old-seed", oldSeed));
        deleteFolder(folder);
        World fresh = server.createWorld(creator);
        if (fresh == null) throw new IllegalStateException(Messages.text("console.world-reset.recreate-failed"));
        if (fresh.getSeed() != newSeed) {
            throw new IllegalStateException(Messages.text("console.world-reset.seed-mismatch", "expected-seed", newSeed, "actual-seed", fresh.getSeed()));
        }
        logger.info(Messages.log("console.world-reset.directory", "path", fresh.getWorldFolder().getAbsolutePath()));
        return fresh;
    }

    private static Path validateFolder(Server server, World world) throws IOException {
        if (!WORLD_NAME.equals(world.getName()) || server.getWorlds().isEmpty()
                || server.getWorlds().getFirst() == world) {
            throw new IllegalStateException(Messages.text("console.world-reset.unsafe-world"));
        }
        Path container = server.getWorldContainer().getCanonicalFile().toPath();
        Path requested = world.getWorldFolder().toPath().toAbsolutePath().normalize();
        Path folder = world.getWorldFolder().getCanonicalFile().toPath();
        if (!folder.startsWith(container) || folder.equals(container)
                || !WORLD_NAME.equals(folder.getFileName().toString())) {
            throw new IOException(Messages.text("console.world-reset.unsafe-directory", "folder", folder));
        }
        // Reject linked arena roots/parents; nested links are never traversed by walkFileTree.
        for (Path component = requested; component != null && !component.equals(container); component = component.getParent()) {
            if (Files.isSymbolicLink(component)) throw new IOException(Messages.text("console.world-reset.linked-directory", "component", component));
        }
        for (World other : server.getWorlds()) {
            if (other == world) continue;
            Path otherFolder = other.getWorldFolder().getCanonicalFile().toPath();
            if (otherFolder.startsWith(folder)) {
                throw new IOException(Messages.text("console.world-reset.nested-world", "player", other.getName()));
            }
        }
        return folder;
    }

    private static void deleteFolder(Path folder) throws IOException {
        if (!Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
