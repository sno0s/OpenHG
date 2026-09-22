package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FeastTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void splitAmountKeepsExactSumAndRespectsMaxStack() {
        int[][] cases = {
                // amount, pieces, maxStack
                {64, 3, 64},
                {64, 12, 64},
                {100, 2, 64},
                {128, 2, 64},
                {10, 10, 1},
                {5, 5, 1},
                {16, 4, 16},
                {7, 1, 64}
        };
        for (long seed = 0; seed < 50; seed++) {
            for (int[] c : cases) {
                int[] parts = Feast.splitAmount(c[0], c[1], c[2], new Random(seed));
                assertEquals(c[1], parts.length);
                int sum = 0;
                for (int part : parts) {
                    assertTrue(part >= 1, "cada pedaço deve ter pelo menos 1 item");
                    assertTrue(part <= c[2], "pedaço " + part + " excede o max stack " + c[2]);
                    sum += part;
                }
                assertEquals(c[0], sum, "soma deve ser exatamente o amount");
            }
        }
    }

    @Test
    void cobwebAmount64IsSpreadAcrossMultipleChests() {
        for (long seed = 0; seed < 50; seed++) {
            List<Inventory> chests = new ArrayList<>();
            for (int i = 0; i < 12; i++) chests.add(server.createInventory(null, Feast.CHEST_SLOTS));
            List<Feast.LootEntry> loot = List.of(new Feast.LootEntry(Material.COBWEB, 64));

            int placed = Feast.distributeLoot(chests, loot, new Random(seed));

            int total = 0;
            int stacks = 0;
            int chestsWithCobweb = 0;
            for (Inventory chest : chests) {
                int inChest = 0;
                for (ItemStack item : chest.getContents()) {
                    if (item == null || item.getType() != Material.COBWEB) continue;
                    assertTrue(item.getAmount() <= Material.COBWEB.getMaxStackSize());
                    total += item.getAmount();
                    stacks++;
                    inChest++;
                }
                assertTrue(inChest <= 1, "cada pedaço deve ir para um baú diferente");
                if (inChest > 0) chestsWithCobweb++;
            }

            assertEquals(64, total, "total de cobweb deve ser exato");
            assertEquals(placed, stacks);
            assertTrue(stacks >= Feast.MIN_SPREAD, "64 cobwebs devem virar pelo menos " + Feast.MIN_SPREAD + " porções");
            assertEquals(stacks, chestsWithCobweb);
        }
    }

    @Test
    void nonStackableAmountCanUseMultipleSlotsPerChest() {
        List<Inventory> chests = new ArrayList<>();
        for (int i = 0; i < 12; i++) chests.add(server.createInventory(null, Feast.CHEST_SLOTS));
        List<Feast.LootEntry> loot = List.of(new Feast.LootEntry(Material.DIAMOND_SWORD, 30));

        int placed = Feast.distributeLoot(chests, loot, new Random(7));

        int total = 0;
        for (Inventory chest : chests) for (ItemStack item : chest.getContents())
            if (item != null && item.getType() == Material.DIAMOND_SWORD) {
                assertEquals(1, item.getAmount());
                total += item.getAmount();
            }
        assertEquals(30, total);
        assertEquals(30, placed);
    }
}
