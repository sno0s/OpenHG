package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.database.PlayerStatsDAO;
import br.dev.sno0s.hgplugin.utils.CraftyAPI;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (GameState.getInstance().getPhase() != MatchPhase.IN_PROGRESS) return;

        Player dead = event.getEntity();
        GameState state = GameState.getInstance();
        PlayerStatsDAO dao = Hgplugin.getStatsDAO();

        GameState.PlayerData deadData = state.getPlayer(dead.getUniqueId());
        if (deadData != null) deadData.setAlive(false);
        if (dao != null) dao.addDeath(dead.getUniqueId());

        Player killer = dead.getKiller();
        if (killer != null) {
            GameState.PlayerData killerData = state.getPlayer(killer.getUniqueId());
            if (killerData != null) {
                killerData.addKill();
                Messages.success(killer, "Você eliminou " + Messages.hl(dead.getName())
                        + "! Kills: " + Messages.hl(String.valueOf(killerData.getKills())));
            }
            if (dao != null) dao.addKill(killer.getUniqueId());
        }

        Bukkit.getScheduler().runTaskLater(Hgplugin.getInstance(), () ->
                dead.setGameMode(GameMode.SPECTATOR), 1L);

        checkWinCondition(state);
    }

    private void checkWinCondition(GameState state) {
        long alivePlayers = state.getPlayers().values().stream()
                .filter(GameState.PlayerData::isAlive)
                .count();

        if (alivePlayers > 1) return;

        state.setPhase(MatchPhase.ENDED);

        PlayerStatsDAO dao = Hgplugin.getStatsDAO();

        state.getPlayers().forEach((uuid, data) -> {
            if (dao != null) dao.addMatch(uuid);
        });

        state.getPlayers().entrySet().stream()
                .filter(e -> e.getValue().isAlive())
                .findFirst()
                .ifPresentOrElse(e -> {
                    if (dao != null) dao.addWin(e.getKey());
                    Messages.broadcast(Messages.hl(e.getValue().getName()) + " venceu a partida!");
                }, () -> Messages.broadcast("Empate! Ninguém sobreviveu."));

        CraftyAPI.scheduleRestart();
    }
}
