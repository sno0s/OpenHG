package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.GameRule;
import org.bukkit.World;

/** Mantém a arena diurna e sem chuva, usando a instância criada pelo servidor. */
public final class ArenaEnvironment {
    private ArenaEnvironment() {}

    public static void apply(World world) {
        if (!Boolean.FALSE.equals(world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE))) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }
        if (!Boolean.FALSE.equals(world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE))) {
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        }
        if (world.getTime() != 1000L) world.setTime(1000L);
        if (world.hasStorm()) world.setStorm(false);
        if (world.isThundering()) world.setThundering(false);
    }
}
