package br.dev.sno0s.hgplugin.items;

import br.dev.sno0s.hgplugin.listeners.SwordBlockingListener;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.registry.keys.DamageTypeKeys;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SwordBlockingTest {
    private ServerMock server;
    private JavaPlugin plugin;
    @BeforeEach void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    @Test void everyVanillaSwordBlocksImmediatelyWithoutShieldCooldownOrWear() {
        var service = new SwordBlocking(true, 1f);
        for (Material type : List.of(Material.WOODEN_SWORD, Material.STONE_SWORD,
                Material.COPPER_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD,
                Material.DIAMOND_SWORD, Material.NETHERITE_SWORD)) {
            var sword = new ItemStack(type);
            assertTrue(service.apply(sword));
            BlocksAttacks blocking = sword.getData(DataComponentTypes.BLOCKS_ATTACKS);
            assertNotNull(blocking);
            assertEquals(0f, blocking.blockDelaySeconds());
            assertEquals(0f, blocking.disableCooldownScale());
            assertEquals(0, blocking.itemDamage().damageToApply(10f));
            var reduction = blocking.damageReductions().getFirst();
            assertEquals(1f, reduction.base());
            assertEquals(0f, reduction.factor());
            assertEquals(180f, reduction.horizontalBlockingAngle());
            assertTrue(reduction.type().contains(DamageTypeKeys.PLAYER_ATTACK));
            assertTrue(reduction.type().contains(DamageTypeKeys.ARROW));
            assertFalse(reduction.type().contains(DamageTypeKeys.FALL));
            assertFalse(reduction.type().contains(DamageTypeKeys.ON_FIRE));
            assertFalse(service.apply(sword), "no repeated writes");
        }
    }
    @Test void preservesCustomSwordAndLeavesOtherItemsAlone() {
        var service = new SwordBlocking(true, 1f);
        var sword = new ItemStack(Material.DIAMOND_SWORD);
        var meta = (Damageable) sword.getItemMeta();
        meta.setDisplayName("Espada pessoal");
        meta.setLore(List.of("Descrição original"));
        meta.setDamage(17);
        meta.addEnchant(Enchantment.SHARPNESS, 3, true);
        PluginItems.mark(meta, "custom-sword");
        sword.setItemMeta(meta);
        service.apply(sword);
        assertEquals("Espada pessoal", sword.getItemMeta().getDisplayName());
        assertEquals(List.of("Descrição original"), sword.getItemMeta().getLore());
        assertEquals(17, ((Damageable) sword.getItemMeta()).getDamage());
        assertEquals(3, sword.getEnchantmentLevel(Enchantment.SHARPNESS));
        assertEquals("custom-sword", PluginItems.id(sword));
        assertFalse(service.apply(new ItemStack(Material.WOODEN_AXE)));
        assertFalse(service.apply(new ItemStack(Material.SHIELD)));
        assertFalse(service.apply(null));
    }
    @Test void disablingRemovesOnlyOurComponentAndUpdatesExistingSwords() {
        var sword = new ItemStack(Material.IRON_SWORD);
        new SwordBlocking(true, 1f).apply(sword);
        assertTrue(new SwordBlocking(true, 2f).apply(sword));
        assertEquals(2f, sword.getData(DataComponentTypes.BLOCKS_ATTACKS).damageReductions().getFirst().base());
        assertTrue(new SwordBlocking(false, 1f).apply(sword));
        assertNull(sword.getData(DataComponentTypes.BLOCKS_ATTACKS));
        var other = new ItemStack(Material.DIAMOND_SWORD);
        var foreign = BlocksAttacks.blocksAttacks().blockDelaySeconds(2f).build();
        other.setData(DataComponentTypes.BLOCKS_ATTACKS, foreign);
        assertFalse(new SwordBlocking(true, 1f).apply(other));
        assertFalse(new SwordBlocking(false, 1f).apply(other));
        assertEquals(foreign, other.getData(DataComponentTypes.BLOCKS_ATTACKS));
    }
    @Test void inventoryChangesCoverLootWithoutRepeatedRewrites() {
        // MockBukkit 4.116.1 perde DataComponents ao clonar itens no inventário.
        // Valide o item entregue à API, usando armazenamento que preserva os componentes.
        var player = mock(org.bukkit.entity.Player.class);
        var inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        var sword = new ItemStack(Material.DIAMOND_SWORD);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getSize()).thenReturn(41);
        when(inventory.getItem(12)).thenReturn(sword);
        var listener = new SwordBlockingListener(plugin, new SwordBlocking(true, 1f));
        var event = mock(PlayerInventorySlotChangeEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getNewItemStack()).thenReturn(sword);
        listener.onSlotChanged(event);
        listener.onSlotChanged(event);
        server.getScheduler().performOneTick();
        assertNotNull(sword.getData(DataComponentTypes.BLOCKS_ATTACKS));
        listener.updateInventory(player);
        verify(inventory, times(1)).setItem(12, sword);
    }

    @Test void cancelsShieldRecoilOnlyForOurBlockingSword() {
        var blocking = new SwordBlocking(true, 1f);
        var listener = new SwordBlockingListener(plugin, blocking);
        var sword = new ItemStack(Material.IRON_SWORD);
        blocking.apply(sword);
        var defender = mock(org.bukkit.entity.Player.class);
        when(defender.getActiveItem()).thenReturn(sword);
        var attacker = server.addPlayer();
        var recoil = new io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent(attacker,
                io.papermc.paper.event.entity.EntityKnockbackEvent.Cause.SHIELD_BLOCK,
                defender, new org.bukkit.util.Vector(1, 0, 0));
        listener.onShieldRecoil(recoil);
        assertTrue(recoil.isCancelled());
        var regular = new io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent(attacker,
                io.papermc.paper.event.entity.EntityKnockbackEvent.Cause.ENTITY_ATTACK,
                defender, new org.bukkit.util.Vector(1, 0, 0));
        listener.onShieldRecoil(regular);
        assertFalse(regular.isCancelled());
        when(defender.getActiveItem()).thenReturn(new ItemStack(Material.SHIELD));
        recoil.setCancelled(false);
        listener.onShieldRecoil(recoil);
        assertFalse(recoil.isCancelled());
    }

    @Test void craftingResultIsPreparedBeforeThePlayerUsesIt() {
        var listener = new SwordBlockingListener(plugin, new SwordBlocking(true, 1f));
        var event = mock(org.bukkit.event.inventory.PrepareItemCraftEvent.class);
        var inventory = mock(org.bukkit.inventory.CraftingInventory.class);
        var sword = new ItemStack(Material.STONE_SWORD);
        when(event.getInventory()).thenReturn(inventory);
        when(inventory.getResult()).thenReturn(sword);
        listener.onCraft(event);
        assertNotNull(sword.getData(DataComponentTypes.BLOCKS_ATTACKS));
        verify(inventory).setResult(sword);
    }

    @Test void invalidReductionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SwordBlocking(true, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new SwordBlocking(true, -1));
    }
}
