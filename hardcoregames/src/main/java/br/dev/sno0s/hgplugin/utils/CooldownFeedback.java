package br.dev.sno0s.hgplugin.utils;

import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Feedback comum de cooldown: aviso no chat e barra de progresso na action bar. */
public final class CooldownFeedback {
    private static final int BAR_SIZE = 20;
    private static final Map<UUID, BukkitTask> TASKS = new HashMap<>();

    private CooldownFeedback() {}

    public static void start(Player player, String kit, long durationMillis) {
        if (durationMillis <= 0) return;
        BukkitTask previous = TASKS.remove(player.getUniqueId());
        if (previous != null) previous.cancel();
        var plugin = Hgplugin.getInstance();
        if (plugin == null) return;
        long startedAt = System.currentTimeMillis();
        BukkitTask task = new BukkitRunnable() {
            @Override public void run() {
                long remaining = durationMillis - (System.currentTimeMillis() - startedAt);
                if (remaining <= 0 || !player.isOnline()) {
                    TASKS.remove(player.getUniqueId());
                    player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(""));
                    cancel();
                    return;
                }
                int seconds = (int) ((remaining + 999) / 1000);
                double ratio = Math.max(0, Math.min(1, remaining / (double) durationMillis));
                int filled = (int) Math.ceil(ratio * BAR_SIZE);
                String bar = "§a" + "▌".repeat(filled) + "§7" + "▌".repeat(BAR_SIZE - filled);
                String text = Messages.text("kits.cooldown-bar", "kit", kit, "seconds", seconds, "bar", bar);
                player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
            }
        }.runTaskTimer(plugin, 0L, 2L);
        TASKS.put(player.getUniqueId(), task);
    }
}
