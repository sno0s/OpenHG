package br.dev.sno0s.hgplugin.utils;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public class NearestPlayer {

    /**
     * Retorna o Player mais próximo da Location `origin` dentro de `radius` blocos.
     * Se `ignore` for não-nulo, esse player será ignorado (útil para procurar "outro jogador").
     * Retorna null se não houver ninguém no raio.
     */
    public static Player findNearestPlayer(Location origin, double radius, Player ignore) {
        if (origin == null || origin.getWorld() == null) return null;

        Collection<Entity> nearby = origin.getWorld().getNearbyEntities(origin, radius, radius, radius);

        Optional<Player> nearest = nearby.stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                // filter invalid players (dead, offline or spec) and ignores the player itself, if informed
                .filter(p -> p.isOnline() && !p.isDead())
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .filter(p -> ignore == null || !p.getUniqueId().equals(ignore.getUniqueId()))
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(origin)));

        return nearest.orElse(null);
    }
}
