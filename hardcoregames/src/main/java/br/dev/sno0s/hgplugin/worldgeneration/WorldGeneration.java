package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Chunk;
import org.codehaus.plexus.util.FileUtils;
import java.io.File;
import java.io.IOException;

public class WorldGeneration {

    public static void execute(JavaPlugin plugin) {
        String worldName = "hg_world";
        File hgDirectory = new File(Bukkit.getServer().getWorldContainer(), worldName);

        Bukkit.getLogger().info("[HardcoreGames] Iniciando geração do mundo!");
        long inicio = System.currentTimeMillis();
        // Se já existe, descarrega e apaga
        if (hgDirectory.exists()) {
            Bukkit.unloadWorld(worldName, false);
            try {
                FileUtils.deleteDirectory(hgDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Cria o mundo com chunk generator próprio (sem oceanos) e biomas customizados
        WorldCreator wc = new WorldCreator(worldName);
        wc.environment(World.Environment.NORMAL);
        wc.generator(new HGChunkGenerator());
        wc.biomeProvider(new HGWorldProvider());
        World world = Bukkit.createWorld(wc);

        Bukkit.getLogger().info("[HardcoreGames] WorldBorder e biomas aplicados.");

        Bukkit.getLogger().info("[HardcoreGames] Adicionando cogumelos.");
        world.getPopulators().add(new MushroomPopulator());

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
            MapComponents.gerarParede(plugin, world, 500);
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
