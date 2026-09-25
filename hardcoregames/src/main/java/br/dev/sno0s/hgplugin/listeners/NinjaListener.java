package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.MatchPhase;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Teleporta o Ninja para o último jogador que ele acertou. */
public final class NinjaListener implements Listener {
    private static final double MAX_RANGE = 50.0;
    private final long cooldownMillis;
    private final Map<UUID, UUID> lastTargets = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public NinjaListener(int cooldownSeconds) {
        this.cooldownMillis = cooldownSeconds * 1000L;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player ninja)
                || !(event.getEntity() instanceof Player target)
                || !isNinja(ninja) || !isParticipant(target)) return;
        if (!ninja.getWorld().equals(target.getWorld())
                || ninja.getLocation().distanceSquared(target.getLocation()) > MAX_RANGE * MAX_RANGE) return;
        lastTargets.put(ninja.getUniqueId(), target.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking() || !isNinja(event.getPlayer())) return;

        Player ninja = event.getPlayer();
        UUID uuid = ninja.getUniqueId();
        long now = System.currentTimeMillis();
        long availableAt = cooldowns.getOrDefault(uuid, 0L);
        if (now < availableAt) return;

        UUID targetId = lastTargets.get(uuid);
        Player target = targetId == null ? null : org.bukkit.Bukkit.getPlayer(targetId);
        if (target == null || !isParticipant(target) || !ninja.getWorld().equals(target.getWorld())
                || ninja.getLocation().distanceSquared(target.getLocation()) > MAX_RANGE * MAX_RANGE) return;

        if (ninja.teleport(target.getLocation(), TeleportCause.PLUGIN)) {
            cooldowns.put(uuid, now + cooldownMillis);
        }
    }

    private static boolean isNinja(Player player) {
        GameState state = GameState.getInstance();
        if (state == null || state.getPhase() != MatchPhase.IN_PROGRESS) return false;
        var data = state.getPlayer(player.getUniqueId());
        return data != null && "Ninja".equals(data.getSelectedKit()) && isParticipant(player);
    }

    private static boolean isParticipant(Player player) {
        GameState state = GameState.getInstance();
        var data = state == null ? null : state.getPlayer(player.getUniqueId());
        return data != null && data.isAlive() && player.isOnline() && !player.isDead()
                && player.getGameMode() == GameMode.SURVIVAL && !player.isInvulnerable();
    }
}
