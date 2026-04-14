package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.database.PlayerStatsDAO;
import br.dev.sno0s.hgplugin.utils.CraftyAPI;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisconnectListener implements Listener {

    private static final int TIMEOUT_SECONDS = 30;
    private static final Map<UUID, BukkitTask> pendingEliminations = new HashMap<>();

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (GameState.getInstance().getPhase() != MatchPhase.IN_PROGRESS) return;

        Player player = event.getPlayer();
        GameState.PlayerData data = GameState.getInstance().getPlayer(player.getUniqueId());
        if (data == null || !data.isAlive()) return;

        String name = player.getName();
        Messages.broadcast(Messages.hl(name) + " desconectou. "
                + Messages.hl(TIMEOUT_SECONDS + "s") + " para ser desclassificado.");

        BukkitTask task = Bukkit.getScheduler().runTaskLater(Hgplugin.getInstance(), () -> {
            pendingEliminations.remove(player.getUniqueId());
            data.setAlive(false);
            Messages.broadcast(Messages.hl(name) + " foi desclassificado por desconexão.");
            checkWinCondition();
        }, 20L * TIMEOUT_SECONDS);

        pendingEliminations.put(player.getUniqueId(), task);
    }

    public static void cancelElimination(UUID uuid) {
        BukkitTask task = pendingEliminations.remove(uuid);
        if (task != null) task.cancel();
    }

    private void checkWinCondition() {
        GameState state = GameState.getInstance();

        long alive = state.getPlayers().values().stream()
                .filter(GameState.PlayerData::isAlive)
                .count();

        if (alive > 1) return;

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
