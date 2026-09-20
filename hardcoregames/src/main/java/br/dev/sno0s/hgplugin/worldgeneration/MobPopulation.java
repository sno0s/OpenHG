package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.SpawnCategory;

/** Applies arena-only population limits to every category supported by Bukkit. */
public final class MobPopulation {
    private MobPopulation() {}

    public static void configure(World world, double multiplier) {
        for (SpawnCategory category : SpawnCategory.values()) {
            if (category == SpawnCategory.MISC) continue;
            int normal = world.getSpawnLimit(category);
            if (normal < 0) normal = Bukkit.getSpawnLimit(category);
            world.setSpawnLimit(category, (int) Math.floor(Math.max(0, normal) * multiplier));
        }
    }
}
