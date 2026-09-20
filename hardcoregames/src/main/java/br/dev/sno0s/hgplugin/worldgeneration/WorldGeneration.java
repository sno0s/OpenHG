package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Chunk;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.ConfigManager;
import br.dev.sno0s.hgplugin.listeners.ChunkDecorationListener;

public class WorldGeneration {

    public static void execute(JavaPlugin plugin, ChunkDecorationListener decorations) {
        String worldName = "hg_world";

        Bukkit.getLogger().info(Messages.log("console.world-generation.starting"));
        long inicio = System.currentTimeMillis();
        ConfigManager config = Hgplugin.getConfigManager();
        TerrainProfile terrain = config.getTerrainProfile();
        // Uma seed nova por reinício; todos os chunks usam a mesma seed durante a partida.
        WorldCreator wc = new WorldCreator(worldName);
        wc.seed(ThreadLocalRandom.current().nextLong());
        wc.environment(World.Environment.NORMAL);
        wc.generator(new HGChunkGenerator(terrain, config.getTreeDensity(), config.getMobSpawnMultiplier()));
        wc.biomeProvider(new HGWorldProvider(terrain));
        wc.generateStructures(true);
        World world;
        try {
            world = WorldReset.recreate(plugin.getServer(), wc, plugin.getLogger());
        } catch (IOException e) {
            throw new IllegalStateException(Messages.text("console.world-generation.cleanup-failed"), e);
        }

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(config.getWorldSize());
        MobPopulation.configure(world, config.getMobSpawnMultiplier());
        Bukkit.getLogger().info(Messages.log("console.world-generation.border-applied"));

        Bukkit.getLogger().info(Messages.log("console.world-generation.seed", "seed", world.getSeed()));

        // Spawn central
        world.setSpawnLocation(new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0));

        // Limpa chunks de spawn já carregados (decorações vanilla são aplicadas durante createWorld)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getLogger().info(Messages.log("console.world-generation.cleaning-spawn"));
            for (Chunk chunk : world.getLoadedChunks()) {
                decorations.decorateChunk(chunk);
            }
        }, 20L); // 1s — suficiente para o Paper finalizar decorações

        // Agenda a parede alguns ticks depois (evita travar no onEnable)
        int wallSize = config.getWorldSize();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getLogger().info(Messages.log("console.world-generation.building-wall"));
            MapComponents.gerarParede(plugin, world, wallSize, terrain, config.getWallHeight());
        }, 40L); // ~2s

        Bukkit.getLogger().info(Messages.log("console.world-generation.setting-weather"));
        //configs de tempo
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setTime(1000);
        //configs de clima
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setStorm(false);
        world.setThundering(false);

        //tempo final de contagem
        long fim = System.currentTimeMillis();
        long duracao = fim - inicio;

        Bukkit.getLogger().info(Messages.log("console.world-generation.completed", "milliseconds", duracao));
    }
}
