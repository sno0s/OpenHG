package br.dev.sno0s.hgplugin.utils;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.listeners.PlayerJoinListener;
import br.dev.sno0s.hgplugin.items.Compass;
import br.dev.sno0s.hgplugin.database.PlayerStatsDAO;
import br.dev.sno0s.hgplugin.kits.Kit;
import br.dev.sno0s.hgplugin.kits.KitRegistry;
import br.dev.sno0s.hgplugin.worldgeneration.Feast;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

import java.util.Collection;

public class StartMatch {

    /*
        if all players are set in, the match will start
        - clean inventory
        - set gamemode survival
        - set kit items
        - set 1 min 45 sec invencibility
     */


    public static void execute() {
        Collection<? extends Player> jogadores = Bukkit.getOnlinePlayers();
        Plugin plugin = Hgplugin.getInstance();

        GameState state = GameState.getInstance();
        state.setPhase(MatchPhase.IN_PROGRESS);

        // players initial configs, in invincibility
        for (Player p : jogadores) {
            state.registerPlayer(p); // garante que todos estão registrados
            PlayerJoinListener.removeAttackCooldown(p);
            p.setInvulnerable(true);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();

            GameState.PlayerData data = state.getPlayer(p.getUniqueId());
            if (data != null && data.getSelectedKit() != null) {
                Kit kit = KitRegistry.getByName(data.getSelectedKit());
                if (kit != null) {
                    kit.apply(p);
                    PlayerStatsDAO dao = Hgplugin.getStatsDAO();
                    if (dao != null) dao.updateLastKit(p.getUniqueId(), kit.getName());
                }
            }

            p.getInventory().setItem(8, Compass.create());
        }

        // invincibility countdown
        new BukkitRunnable() {
            int countdown = 105; // 1m45s

            @Override
            public void run() {
                if (countdown == 105 || countdown == 30 || countdown == 15
                        || (countdown <= 5 && countdown > 0)) {
                    Messages.broadcastInfo("Faltam " + Messages.hl(countdown + "s") + " de invencibilidade.");
                }

                if (countdown == 0) {
                    Messages.broadcastInfo("A invencibilidade acabou!");

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.setInvulnerable(false);

                    }
                    cancel(); // Para o contador
                    return;
                }

                countdown--; // decrementa a cada segundo
            }
        }.runTaskTimer(plugin, 0L, 20L); // 20 ticks = 1 segundo

        // Localização aleatória do Feast — mínimo 60 blocos da borda (mapa 500x500)
        int range = 250 - 60; // 190
        Random rng = new Random();
        int feastX = rng.nextInt(range * 2 + 1) - range;
        int feastZ = rng.nextInt(range * 2 + 1) - range;

        String feastCoords = Messages.hl("X: " + feastX + " Z: " + feastZ);

        // Aviso 5 minutos antes
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            Messages.broadcast("O Feast spawna em " + Messages.hl("5 minutos") + " em " + feastCoords + "!"),
        20L * 60 * 5); // 6000 ticks

        // Aviso 1 minuto antes
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            Messages.broadcast("O Feast spawna em " + Messages.hl("1 minuto") + " em " + feastCoords + "!"),
        20L * 60 * 9); // 10800 ticks

        // Contagem de 15 segundos (começa em 9min45s)
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            new BukkitRunnable() {
                int sec = 15;
                @Override
                public void run() {
                    if (sec > 0) {
                        Messages.broadcast("Feast em " + Messages.hl(sec + "s") + "!");
                        sec--;
                    } else {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L),
        20L * (60 * 9 + 45)); // 11700 ticks

        // Spawn do Feast aos 10 minutos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World world = Bukkit.getWorld("hg_world");
            if (world != null) {
                Feast.spawnFeast(new Location(world, feastX, 0, feastZ));
                Messages.broadcast("O Feast spawnou em " + Messages.hl("X: " + feastX + " Z: " + feastZ) + "!");
            }
        }, 20L * 60 * 10); // 12000 ticks
    }
}
