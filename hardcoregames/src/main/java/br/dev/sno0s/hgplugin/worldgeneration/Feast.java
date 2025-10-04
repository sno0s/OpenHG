package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class Feast {

    public void spawnFeast(Location loc) {
        World world = loc.getWorld();
        int r = 30;
        int y = world.getHighestBlockYAt(loc);
        Location center = new Location(world, loc.getX(), y, loc.getZ());

        Location enchant = new Location(world, center.getX(), center.getY() + 1, center.getZ());

        Location bau1 = new Location(world, center.getX(), center.getY() + 1, center.getZ() + 2);
        Location bau2 = new Location(world, center.getX() + 2, center.getY() + 1, center.getZ() + 2);
        Location bau3 = new Location(world, center.getX() - 2, center.getY() + 1, center.getZ() + 2);
        Location bau4 = new Location(world, center.getX() - 1, center.getY() + 1, center.getZ() + 1);
        Location bau5 = new Location(world, center.getX() + 1, center.getY() + 1, center.getZ() + 1);
        Location bau6 = new Location(world, center.getX() - 2, center.getY() + 1, center.getZ());
        Location bau7 = new Location(world, center.getX() + 2, center.getY() + 1, center.getZ());
        Location bau8 = new Location(world, center.getX() - 1, center.getY() + 1, center.getZ() - 1);
        Location bau9 = new Location(world, center.getX() + 1, center.getY() + 1, center.getZ() - 1);
        Location bau10 = new Location(world, center.getX() - 2, center.getY() + 1, center.getZ() - 2);
        Location bau11 = new Location(world, center.getX(), center.getY() + 1, center.getZ() - 2);
        Location bau12 = new Location(world, center.getX() + 2, center.getY() + 1, center.getZ() - 2);

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx*dx + dz*dz;
                if (d2 <= r*r) {
                    int x = center.getBlockX() + dx;
                    int z = center.getBlockZ() + dz;
                    world.getBlockAt(x, y, z).setType(Material.GRASS_BLOCK, false); // Paper: sem física
                }
            }
        }

        enchant.getBlock().setType(Material.ENCHANTING_TABLE);
        bau1.getBlock().setType(Material.CHEST);
        bau2.getBlock().setType(Material.CHEST);
        bau3.getBlock().setType(Material.CHEST);
        bau4.getBlock().setType(Material.CHEST);
        bau5.getBlock().setType(Material.CHEST);
        bau6.getBlock().setType(Material.CHEST);
        bau7.getBlock().setType(Material.CHEST);
        bau8.getBlock().setType(Material.CHEST);
        bau9.getBlock().setType(Material.CHEST);
        bau10.getBlock().setType(Material.CHEST);
        bau11.getBlock().setType(Material.CHEST);
        bau12.getBlock().setType(Material.CHEST);
    }

}
