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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Feast {

    static class LootEntry {
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

    static final int CHEST_SLOTS = 27;
    // Quantidades a partir deste valor são espalhadas por pelo menos esse número de baús.
    static final int MIN_SPREAD = 3;

    static int distributeLoot(List<Inventory> chests, List<LootEntry> loot, Random rng) {
        int totalSlots = chests.size() * CHEST_SLOTS;
        int minPieces = 0;
        for (LootEntry entry : loot) minPieces += minPieces(entry);
        if (minPieces > totalSlots) {
            Bukkit.getLogger().warning(Messages.log("console.feast.loot-capacity", "items", minPieces, "slots", totalSlots));
            return 0;
        }

        // Pedaços extras além do mínimo consomem slots livres; o orçamento impede estourar a capacidade.
        int budget = totalSlots - minPieces;
        List<List<Integer>> free = new ArrayList<>();
        for (int chest = 0; chest < chests.size(); chest++) {
            List<Integer> slots = new ArrayList<>();
            for (int slot = 0; slot < CHEST_SLOTS; slot++) slots.add(slot);
            Collections.shuffle(slots, rng);
            free.add(slots);
        }

        int placed = 0;
        for (LootEntry entry : loot) {
            if (entry.amount <= 0) continue;
            int min = minPieces(entry);
            // Itens não empilháveis podem precisar de mais porções que o número de baús;
            // nesse caso, vários slots do mesmo baú são válidos.
            int upper = Math.max(min, Math.min(entry.amount, chests.size()));
            int lower = Math.max(min, Math.min(upper, MIN_SPREAD));
            int pieces = lower == upper ? lower
                    : Math.min(lower + rng.nextInt(upper - lower + 1), min + budget);
            budget -= pieces - min;

            List<Integer> order = new ArrayList<>();
            for (int chest = 0; chest < chests.size(); chest++) order.add(chest);
            Collections.shuffle(order, rng);
            int cursor = 0;
            for (int amount : splitAmount(entry.amount, pieces, maxStack(entry), rng)) {
                // cada pedaço vai para um baú diferente; baús cheios são pulados
                while (free.get(order.get(cursor % order.size())).isEmpty()) cursor++;
                int chest = order.get(cursor++ % order.size());
                chests.get(chest).setItem(free.get(chest).removeLast(), new ItemStack(entry.material, amount));
                placed++;
            }
        }
        return placed;
    }

    private static int maxStack(LootEntry entry) {
        return Math.max(1, entry.material.getMaxStackSize());
    }

    private static int minPieces(LootEntry entry) {
        return Math.max(0, entry.amount + maxStack(entry) - 1) / maxStack(entry);
    }

    // Divide amount em `pieces` partes positivas, cada uma <= maxStack, com soma exata.
    static int[] splitAmount(int amount, int pieces, int maxStack, Random rng) {
        int[] parts = new int[pieces];
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < pieces; i++) {
            parts[i] = 1;
            if (maxStack > 1) open.add(i);
        }
        for (int left = amount - pieces; left > 0; left--) {
            int pick = rng.nextInt(open.size());
            int index = open.get(pick);
            if (++parts[index] == maxStack) {
                int last = open.removeLast();
                if (pick < open.size()) open.set(pick, last);
            }
        }
        return parts;
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
            List<Inventory> chests = new ArrayList<>();
            for (int[] offset : CHEST_OFFSETS) {
                Block block = world.getBlockAt(cx + offset[0], cy, cz + offset[1]);
                if (block.getState() instanceof Chest chest) chests.add(chest.getBlockInventory());
            }
            int stacks = distributeLoot(chests, loot, random);
            Bukkit.getLogger().info(Messages.log("console.feast.loot-distributed", "filled", chests.size(), "stacks", stacks));
        }, 2L);
    }
}
