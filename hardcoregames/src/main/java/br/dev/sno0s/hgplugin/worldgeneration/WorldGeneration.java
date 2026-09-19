package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Chunk;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.ConfigManager;

public class WorldGeneration {

    public static void execute(JavaPlugin plugin) {
        String worldName = "hg_world";

        Bukkit.getLogger().info("[HardcoreGames] Iniciando geração do mundo!");
        long inicio = System.currentTimeMillis();
        ConfigManager config = Hgplugin.getConfigManager();
        TerrainProfile terrain = config.getTerrainProfile();
        // Uma seed nova por reinício; todos os chunks usam a mesma seed durante a partida.
        WorldCreator wc = new WorldCreator(worldName);
        wc.seed(ThreadLocalRandom.current().nextLong());
        wc.environment(World.Environment.NORMAL);
        wc.generator(new HGChunkGenerator(terrain, config.getTreeDensity(), config.getMushroomDensity()));
        wc.biomeProvider(new HGWorldProvider(terrain));
        wc.generateStructures(false);
        World world;
        try {
            world = WorldReset.recreate(plugin.getServer(), wc, plugin.getLogger());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao limpar os dados antigos de hg_world.", e);
        }

        Bukkit.getLogger().info("[HardcoreGames] WorldBorder e biomas aplicados.");

        Bukkit.getLogger().info("[HardcoreGames] Seed do mapa: " + world.getSeed());

        // Spawn central
        world.setSpawnLocation(new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0));

        // Limpa chunks de spawn já carregados (decorações vanilla são aplicadas durante createWorld)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getLogger().info("[HardcoreGames] Limpando chunks de spawn.");
            for (Chunk chunk : world.getLoadedChunks()) {
                TrashCleanPopulator.cleanChunk(chunk);
            }
        }, 20L); // 1s — suficiente para o Paper finalizar decorações

        // Agenda a parede alguns ticks depois (evita travar no onEnable)
        int wallSize = 500;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getLogger().info("[HardcoreGames] Gerando bordas.");
            MapComponents.gerarParede(plugin, world, wallSize, terrain, config.getWallHeight());
        }, 40L); // ~2s

        Bukkit.getLogger().info("[HardcoreGames] Setando configurações de clima e tempo.");
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

        Bukkit.getLogger().info("Geração do mundo demorou: " + duracao + " ms para rodar.");
    }
}
