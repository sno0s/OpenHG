package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Feast {

    private static class LootEntry {
        Material material;
        int chance, min, max;

        LootEntry(Material material, int chance, int min, int max) {
            this.material = material;
            this.chance = chance;
            this.min = min;
            this.max = max;
        }
    }

    private static final Random random = new Random();

    // Xadrez 5x5 centrado na enchanting table — baús onde (dx+dz) é par, exceto o centro
    // C . C . C
    // . C . C .
    // C . E . C
    // . C . C .
    // C . C . C
    private static final int[][] CHEST_OFFSETS = {
            {-2, -2}, { 0, -2}, { 2, -2},
            {-1, -1}, { 1, -1},
            {-2,  0}, { 2,  0},
            {-1,  1}, { 1,  1},
            {-2,  2}, { 0,  2}, { 2,  2}
    };

    private static List<LootEntry> loadLoot() {
        List<LootEntry> loot = new ArrayList<>();

        // getMapList() retorna List<Map<?,?>> corretamente — getList() retornava LinkedHashMap
        // e o cast para ConfigurationSection sempre falhava
        List<Map<?, ?>> list = Hgplugin.getConfigManager().getConfig().getMapList("HGconfigs.feast-loot");

        if (list.isEmpty()) {
            Bukkit.getLogger().warning("[HardcoreGames] feast-loot não configurado, baús vazios.");
            return loot;
        }

        for (Map<?, ?> entry : list) {
            String matName = (String) entry.get("material");
            if (matName == null) continue;

            Material material = Material.matchMaterial(matName);
            if (material == null) {
                Bukkit.getLogger().warning("[HardcoreGames] Material inválido no feast-loot: " + matName);
                continue;
            }

            int chance = entry.containsKey("chance") ? ((Number) entry.get("chance")).intValue() : 50;
            int min    = entry.containsKey("min")    ? ((Number) entry.get("min")).intValue()    : 1;
            int max    = entry.containsKey("max")    ? ((Number) entry.get("max")).intValue()    : 1;

            loot.add(new LootEntry(material, chance, min, max));
        }

        return loot;
    }

    private static void fillChest(Chest chest, List<LootEntry> loot) {
        // getBlockInventory() = referência ao vivo do tile entity, sem necessidade de update()
        for (LootEntry entry : loot) {
            if (random.nextInt(100) < entry.chance) {
                int amount = entry.min + random.nextInt(entry.max - entry.min + 1);
                chest.getBlockInventory().addItem(new ItemStack(entry.material, amount));
            }
        }
    }

    public static void spawnFeast(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            Bukkit.getLogger().warning("[HardcoreGames] Feast: localização inválida.");
            return;
        }

        World world = loc.getWorld();
        int r = 30;
        int y = world.getHighestBlockYAt(loc);
        Location center = new Location(world, loc.getBlockX(), y, loc.getBlockZ());

        Bukkit.getLogger().info("[HardcoreGames] Spawnando Feast em "
                + center.getBlockX() + ", " + y + ", " + center.getBlockZ());

        // aplana área circular
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r) {
                    int bx = center.getBlockX() + dx;
                    int bz = center.getBlockZ() + dz;
                    world.getBlockAt(bx, y,     bz).setType(Material.GRASS_BLOCK, false);
                    world.getBlockAt(bx, y + 1, bz).setType(Material.AIR, false);
                }
            }
        }

        // limpa coluna de ar acima de toda a área circular do feast até o teto do mundo
        int maxY = world.getMaxHeight();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r) {
                    for (int clearY = y + 2; clearY < maxY; clearY++) {
                        Block b = world.getBlockAt(center.getBlockX() + dx, clearY, center.getBlockZ() + dz);
                        if (!b.getType().isAir()) b.setType(Material.AIR, false);
                    }
                }
            }
        }

        // mesa de encantamento no centro (y+1 = sobre a grama)
        world.getBlockAt(center.getBlockX(), y + 1, center.getBlockZ())
                .setType(Material.ENCHANTING_TABLE, false);

        // coloca os 12 baús
        for (int[] offset : CHEST_OFFSETS) {
            world.getBlockAt(center.getBlockX() + offset[0], y + 1, center.getBlockZ() + offset[1])
                    .setType(Material.CHEST, false);
        }

        Bukkit.getLogger().info("[HardcoreGames] Feast spawnado com sucesso!");

        // preenche os baús 2 ticks depois — garante que os tile entities estão inicializados
        List<LootEntry> loot = loadLoot();
        Bukkit.getLogger().info("[HardcoreGames] Itens de loot carregados: " + loot.size());

        if (loot.isEmpty()) return;

        final int cx = center.getBlockX();
        final int cy = y + 1;
        final int cz = center.getBlockZ();

        Bukkit.getScheduler().runTaskLater(Hgplugin.getInstance(), () -> {
            int filled = 0;
            for (int[] offset : CHEST_OFFSETS) {
                Block block = world.getBlockAt(cx + offset[0], cy, cz + offset[1]);
                if (block.getState() instanceof Chest chest) {
                    fillChest(chest, loot);
                    filled++;
                }
            }
            Bukkit.getLogger().info("[HardcoreGames] Loot distribuído em " + filled + " baús.");
        }, 2L);
    }
}
