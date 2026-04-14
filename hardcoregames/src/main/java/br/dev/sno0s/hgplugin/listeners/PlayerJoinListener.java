package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.database.PlayerStatsDAO;
import br.dev.sno0s.hgplugin.items.Compass;
import br.dev.sno0s.hgplugin.utils.Messages;
import br.dev.sno0s.hgplugin.utils.PlayerJoinItems;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        World hgWorld = Bukkit.getWorld("hg_world");
        if (hgWorld == null) return;

        Player player = event.getPlayer();
        Location spawn = hgWorld.getSpawnLocation();
        GameState state = GameState.getInstance();
        MatchPhase phase = state.getPhase();

        Messages.broadcastInfo(Messages.hl(player.getName()) + " entrou no servidor!");

        DisconnectListener.cancelElimination(player.getUniqueId());
        removeAttackCooldown(player);

        PlayerStatsDAO dao = Hgplugin.getStatsDAO();
        if (dao != null) dao.ensureExists(player.getUniqueId(), player.getName());

        player.teleport(spawn);
        player.getInventory().clear();

        if (phase == MatchPhase.WAITING || phase == MatchPhase.COUNTDOWN) {
            state.registerPlayer(player);
            player.setGameMode(GameMode.ADVENTURE);
            player.setInvulnerable(true);
            PlayerJoinItems.give(player);

        } else if (phase == MatchPhase.IN_PROGRESS) {
            GameState.PlayerData data = state.getPlayer(player.getUniqueId());

            if (data != null && data.isAlive()) {
                player.setGameMode(GameMode.SURVIVAL);
                player.setInvulnerable(false);
                player.getInventory().setItem(8, Compass.create());
                Messages.send(player, "Bem-vindo de volta! A partida está em andamento.");
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                player.setInvulnerable(true);
                Messages.send(player, "A partida já começou. Você está no modo espectador.");
            }

        } else { // ENDED
            player.setGameMode(GameMode.SPECTATOR);
            player.setInvulnerable(true);
            Messages.send(player, "A partida terminou. Aguarde o próximo round.");
        }
    }

    public static void removeAttackCooldown(Player player) {
        var attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr != null) attr.setBaseValue(1024.0);
    }
}
