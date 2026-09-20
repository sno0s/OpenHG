package br.dev.sno0s.hgplugin.utils;

import br.dev.sno0s.hgplugin.ConfigManager;
import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CraftyAPI {
    private static final AtomicBoolean RESTART_PENDING = new AtomicBoolean();
    private CraftyAPI() {}

    private static CraftyClient client() {
        ConfigManager cfg = Hgplugin.getConfigManager();
        return new CraftyClient(cfg.getCraftyUrl(), cfg.getCraftyApiKey(), cfg.getCraftyServerId(), 10000);
    }

    public static void checkConnection(CommandSender sender) {
        final CraftyClient client;
        try { client = client(); }
        catch (IllegalArgumentException e) {
            sender.sendMessage("[HardcoreGames] Crafty: " + e.getMessage());
            return;
        }
        sender.sendMessage("[HardcoreGames] Consultando o Crafty...");
        Bukkit.getScheduler().runTaskAsynchronously(Hgplugin.getInstance(), () -> {
            CraftyClient.Result result = client.check();
            Hgplugin.getInstance().getLogger().info("Crafty check: " + result.message());
            Bukkit.getScheduler().runTask(Hgplugin.getInstance(), () ->
                    sender.sendMessage("[HardcoreGames] " + result.message()
                            + (result.success() ? " A consulta confirma acesso ao servidor; a permissão de reinício é verificada ao reiniciar." : "")));
        });
    }

    public static void scheduleRestart() {
        final CraftyClient client;
        try { client = client(); }
        catch (IllegalArgumentException e) {
            Hgplugin.getInstance().getLogger().warning("Crafty: " + e.getMessage());
            Messages.broadcast("Reinício não agendado: confira a configuração do Crafty no console.");
            return;
        }
        if (!RESTART_PENDING.compareAndSet(false, true)) return;
        int delay = Math.max(0, Hgplugin.getConfigManager().getCraftyRestartDelay());
        new BukkitRunnable() {
            int countdown = delay;
            @Override public void run() {
                if (countdown <= 0) {
                    Messages.broadcast("Solicitando reinício ao Crafty...");
                    restartAsync(client);
                    cancel();
                    return;
                }
                if (countdown == delay || countdown <= 5) {
                    Messages.broadcast("Servidor reiniciando em " + Messages.hl(countdown + "s") + "!");
                }
                countdown--;
            }
        }.runTaskTimer(Hgplugin.getInstance(), 0L, 20L);
    }

    private static void restartAsync(CraftyClient client) {
        Bukkit.getScheduler().runTaskAsynchronously(Hgplugin.getInstance(), () -> {
            CraftyClient.Result result = client.restart();
            if (result.success()) {
                Hgplugin.getInstance().getLogger().info("Crafty: reinício aceito. " + result.message());
                // Keep the guard until shutdown, preventing overlapping automatic restarts.
            } else {
                RESTART_PENDING.set(false);
                Hgplugin.getInstance().getLogger().severe("Crafty: " + result.message());
                Bukkit.getScheduler().runTask(Hgplugin.getInstance(), () ->
                        Messages.broadcast("O Crafty não confirmou o reinício. Confira o console ou use /restarthg check."));
            }
        });
    }
}
