package br.dev.sno0s.hgplugin.worldgeneration;

import br.dev.sno0s.hgplugin.utils.Messages;
import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Feast {

    private static class LootEntry {
        Material material;
        int amount;

        LootEntry(Material material, int amount) {
            this.material = material;
            this.amount = amount;
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
            Bukkit.getLogger().warning(Messages.log("console.feast.missing-loot"));
            return loot;
        }

        for (Map<?, ?> entry : list) {
            String matName = (String) entry.get("material");
            if (matName == null) continue;

            Material material = Material.matchMaterial(matName);
            if (material == null) {
                Bukkit.getLogger().warning(Messages.log("console.feast.invalid-material", "material", matName));
                continue;
            }

            if (entry.containsKey("amount")) {
                int amount = Math.max(0, ((Number) entry.get("amount")).intValue());
                loot.add(new LootEntry(material, amount));
                continue;
            }
            // Compatibilidade temporária: o formato antigo decide uma vez por material,
            // mas o novo formato amount é o único que garante totais exatos.
            int chance = entry.containsKey("chance") ? ((Number) entry.get("chance")).intValue() : 50;
            int min = entry.containsKey("min") ? ((Number) entry.get("min")).intValue() : 1;
            int max = entry.containsKey("max") ? ((Number) entry.get("max")).intValue() : min;
            int amount = random.nextInt(100) < Math.clamp(chance, 0, 100)
                    ? min + random.nextInt(Math.max(1, max - min + 1)) : 0;
            loot.add(new LootEntry(material, Math.max(0, amount)));
        }

        return loot;
    }

    static int distributeLoot(List<Chest> chests, List<LootEntry> loot) {
        List<ItemStack> stacks = new ArrayList<>();
        for (LootEntry entry : loot) {
            int left = entry.amount;
            int max = Math.max(1, entry.material.getMaxStackSize());
            while (left > 0) {
                int amount = Math.min(left, max);
                stacks.add(new ItemStack(entry.material, amount));
                left -= amount;
            }
        }
        List<int[]> slots = new ArrayList<>();
        for (int chest = 0; chest < chests.size(); chest++)
            for (int slot = 0; slot < 27; slot++) slots.add(new int[]{chest, slot});
        if (stacks.size() > slots.size()) {
            Bukkit.getLogger().warning(Messages.log("console.feast.loot-capacity", "items", stacks.size(), "slots", slots.size()));
            return 0;
        }
        java.util.Collections.shuffle(slots, random);
        for (int i = 0; i < stacks.size(); i++) {
            int[] target = slots.get(i);
            chests.get(target[0]).getBlockInventory().setItem(target[1], stacks.get(i));
        }
        return stacks.size();
    }

    public static void spawnFeast(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            Bukkit.getLogger().warning(Messages.log("console.feast.invalid-location"));
            return;
        }

        World world = loc.getWorld();
        int r = 30;
        int y = world.getHighestBlockYAt(loc);
        Location center = new Location(world, loc.getBlockX(), y, loc.getBlockZ());

        Bukkit.getLogger().info(Messages.log("console.feast.spawning", "x", center.getBlockX(), "y", y, "z", center.getBlockZ()));

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

        Bukkit.getLogger().info(Messages.log("console.feast.spawned"));

        // preenche os baús 2 ticks depois — garante que os tile entities estão inicializados
        List<LootEntry> loot = loadLoot();
        Bukkit.getLogger().info(Messages.log("console.feast.loot-loaded", "count", loot.size()));

        if (loot.isEmpty()) return;

        final int cx = center.getBlockX();
        final int cy = y + 1;
        final int cz = center.getBlockZ();

        Bukkit.getScheduler().runTaskLater(Hgplugin.getInstance(), () -> {
            List<Chest> chests = new ArrayList<>();
            for (int[] offset : CHEST_OFFSETS) {
                Block block = world.getBlockAt(cx + offset[0], cy, cz + offset[1]);
                if (block.getState() instanceof Chest chest) chests.add(chest);
            }
            int stacks = distributeLoot(chests, loot);
            Bukkit.getLogger().info(Messages.log("console.feast.loot-distributed", "filled", chests.size(), "stacks", stacks));
        }, 2L);
    }
}
