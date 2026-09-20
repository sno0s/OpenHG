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
        try {
            client = client();
        } catch (IllegalArgumentException e) {
            Messages.error(sender, "crafty.error", "detail", e.getMessage());
            return;
        }
        Messages.send(sender, "crafty.checking", "address", client.getAddress());
        Bukkit.getScheduler().runTaskAsynchronously(Hgplugin.getInstance(), () -> {
            CraftyClient.Result result = client.check();
            Hgplugin.getInstance().getLogger().info(Messages.log("console.crafty.check", "detail", result.message()));
            Bukkit.getScheduler().runTask(Hgplugin.getInstance(), () -> {
                if (result.success()) {
                    Messages.success(sender, "crafty.check-success", "detail", result.message());
                } else {
                    Messages.error(sender, "crafty.result", "detail", result.message());
                }
            });
        });
    }

    public static void scheduleRestart() {
        final CraftyClient client;
        try {
            client = client();
        } catch (IllegalArgumentException e) {
            Hgplugin.getInstance().getLogger().warning(Messages.log("console.crafty.configuration-error", "detail", e.getMessage()));
            Messages.broadcast("crafty.restart-not-scheduled");
            return;
        }
        if (!RESTART_PENDING.compareAndSet(false, true)) return;
        int delay = Math.max(0, Hgplugin.getConfigManager().getCraftyRestartDelay());
        new BukkitRunnable() {
            int countdown = delay;
            @Override
            public void run() {
                if (countdown <= 0) {
                    Messages.broadcast("crafty.requesting-restart");
                    restartAsync(client);
                    cancel();
                    return;
                }
                if (countdown == delay || countdown <= 5) {
                    Messages.broadcast("crafty.countdown", "seconds", countdown);
                }
                countdown--;
            }
        }.runTaskTimer(Hgplugin.getInstance(), 0L, 20L);
    }

    private static void restartAsync(CraftyClient client) {
        Bukkit.getScheduler().runTaskAsynchronously(Hgplugin.getInstance(), () -> {
            CraftyClient.Result result = client.restart();
            if (result.success()) {
                Hgplugin.getInstance().getLogger().info(Messages.log("console.crafty.restart-accepted", "detail", result.message()));
                // Mantém a trava até desligar para evitar pedidos de reinício simultâneos.
            } else {
                RESTART_PENDING.set(false);
                Hgplugin.getInstance().getLogger().severe(Messages.log("console.crafty.restart-failed", "detail", result.message()));
                Bukkit.getScheduler().runTask(Hgplugin.getInstance(), () ->
                        Messages.broadcast("crafty.restart-unconfirmed"));
            }
        });
    }
}
