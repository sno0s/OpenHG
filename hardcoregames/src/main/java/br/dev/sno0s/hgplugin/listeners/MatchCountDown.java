package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.utils.Messages;
import br.dev.sno0s.hgplugin.utils.StartMatch;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Set;

public class MatchCountDown implements Listener {

    @EventHandler
    public void onMatchGetMinPlayers(PlayerJoinEvent event) {
        MatchPhase phase = GameState.getInstance().getPhase();
        if (phase != MatchPhase.WAITING) return;

        Collection<? extends Player> jogadores = Bukkit.getOnlinePlayers();
        int numPlayers = jogadores.size();
        Messages.broadcastInfo("Faltam " + Messages.hl(String.valueOf(10 - numPlayers)) + " jogadores para começar!");

        if (numPlayers >= 10) {
            GameState.getInstance().setPhase(MatchPhase.COUNTDOWN);
            Plugin plugin = Hgplugin.getInstance();
            Messages.broadcastInfo("Iniciando contagem para começar a partida...");

            new BukkitRunnable() {
                int countdown = 30;

                @Override
                public void run() {
                    if (Set.of(30, 15, 5, 4, 3, 2, 1).contains(countdown)) {
                        Messages.broadcastInfo("Partida começando em " + Messages.hl(countdown + "s") + "!");
                    }

                    if (countdown == 0) {
                        Messages.broadcast("A partida começou!");

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 0.8f);
                        }

                        StartMatch.execute();
                        cancel();
                        return;
                    }
                    countdown--;
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }
}
