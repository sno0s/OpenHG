package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.Hgplugin;
import br.dev.sno0s.hgplugin.MatchPhase;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Aplica dano direto, uma vez por segundo, somente durante uma partida ativa. */
public final class WallContactListener {
    private WallContactListener() {}

    public static void start(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (GameState.getInstance().getPhase() != MatchPhase.IN_PROGRESS) return;
            World world = Bukkit.getWorld("hg_world");
            if (world == null) return;
            int half = Hgplugin.getConfigManager().getWorldSize() / 2;
            for (Player player : world.getPlayers()) {
                if (player.getGameMode() == GameMode.SPECTATOR || player.isInvulnerable()) continue;
                if (touchingWall(player, half)) player.setHealth(Math.max(0, player.getHealth() - 6.0));
            }
        }, 20L, 20L);
    }

    private static boolean touchingWall(Player player, int half) {
        int minX = player.getLocation().getBlockX() - 1, maxX = player.getLocation().getBlockX() + 1;
        int minZ = player.getLocation().getBlockZ() - 1, maxZ = player.getLocation().getBlockZ() + 1;
        int feet = player.getLocation().getBlockY(), head = feet + 2;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (Math.abs(x) < half - 2 && Math.abs(z) < half - 2) continue;
            for (int y = feet; y <= head; y++) {
                Material type = player.getWorld().getBlockAt(x, y, z).getType();
                if (type.isSolid() && (Math.abs(x) >= half - 2 || Math.abs(z) >= half - 2)) return true;
            }
        }
        return false;
    }
}
