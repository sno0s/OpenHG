package br.dev.sno0s.hgplugin.listeners;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.MatchPhase;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/** Converte uma queda do Stomper em dano verdadeiro de impacto nos arredores. */
public final class StomperListener implements Listener {
    private static final double SELF_FALL_DAMAGE = 1.0; // meio coração
    private final double impactRadius;
    private final double minimumFallHeight;

    public StomperListener(double impactRadius, double minimumFallHeight) {
        this.impactRadius = impactRadius;
        this.minimumFallHeight = minimumFallHeight;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL
                || !(event.getEntity() instanceof Player stomper)
                || !isStomper(stomper)) return;

        double fallHeight = Math.max(0, stomper.getFallDistance());
        event.setDamage(SELF_FALL_DAMAGE);
        // A altura configurada é a referência para matar um jogador cheio (20 HP).
        // Quedas menores continuam causando dano na mesma proporção, sem degrau.
        double impactDamage = Math.max(0, fallHeight / minimumFallHeight * 20.0);
        for (var entity : stomper.getWorld().getNearbyEntities(stomper.getLocation(), impactRadius, impactRadius, impactRadius)) {
            if (!(entity instanceof Player target) || target.equals(stomper) || !isParticipant(target)) continue;
            // setHealth ignora armadura e efeitos de redução; 0 deixa o fluxo de morte do servidor agir.
            target.setHealth(Math.max(0, target.getHealth() - impactDamage));
        }
    }

    private boolean isStomper(Player player) {
        GameState state = GameState.getInstance();
        if (state == null || state.getPhase() != MatchPhase.IN_PROGRESS) return false;
        var data = state.getPlayer(player.getUniqueId());
        return data != null && "Stomper".equals(data.getSelectedKit()) && isParticipant(player);
    }

    private static boolean isParticipant(Player player) {
        GameState state = GameState.getInstance();
        var data = state == null ? null : state.getPlayer(player.getUniqueId());
        return data != null && data.isAlive() && player.isOnline() && !player.isDead()
                && player.getGameMode() == GameMode.SURVIVAL && !player.isInvulnerable();
    }
}
