package br.dev.sno0s.hgplugin.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnchantListenerTest {

    /** Slot do lapis na mesa de encantamento. */
    private static final int LAPIS_SLOT = 1;

    private ServerMock server;
    private WorldMock hgWorld;
    private WorldMock lobby;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        hgWorld = server.addSimpleWorld("hg_world");
        lobby = server.addSimpleWorld("lobby");
        plugin = MockBukkit.createMockPlugin();
        server.getPluginManager().registerEvents(new EnchantListener(), plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ---------------------------------------------------------------- helpers

    private PlayerMock playerIn(WorldMock world) {
        PlayerMock player = server.addPlayer();
        player.teleport(world.getSpawnLocation());
        return player;
    }

    /**
     * Abre uma mesa de encantamento e garante o disparo do InventoryOpenEvent —
     * o MockBukkit nem sempre dispara sozinho. Reabrir e idempotente pro listener.
     */
    private InventoryView openEnchantingTable(PlayerMock player) {
        Inventory table = server.createInventory(null, InventoryType.ENCHANTING);
        InventoryView view = player.openInventory(table);
        assertNotNull(view, "MockBukkit nao abriu a mesa de encantamento");
        server.getPluginManager().callEvent(new InventoryOpenEvent(view));
        return view;
    }

    private void close(InventoryView view) {
        server.getPluginManager().callEvent(new InventoryCloseEvent(view));
    }

    private void quit(PlayerMock player) {
        server.getPluginManager().callEvent(
                new PlayerQuitEvent(player, Component.text("saiu"), PlayerQuitEvent.QuitReason.DISCONNECTED));
    }

    private InventoryClickEvent click(InventoryView view, int rawSlot, InventoryAction action) {
        InventoryClickEvent event = new InventoryClickEvent(
                view, InventoryType.SlotType.CRAFTING, rawSlot, ClickType.LEFT, action);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private InventoryDragEvent drag(InventoryView view, ItemStack cursor, int... rawSlots) {
        Map<Integer, ItemStack> added = new HashMap<>();
        for (int slot : rawSlots) added.put(slot, new ItemStack(Material.LAPIS_LAZULI, 1));

        InventoryDragEvent event = new InventoryDragEvent(
                view, new ItemStack(Material.LAPIS_LAZULI, 1), cursor, false, added);
        server.getPluginManager().callEvent(event);
        return event;
    }

    /** Procura o raw slot do inventario pessoal que contem o material. */
    private int rawSlotOf(InventoryView view, Material material) {
        for (int raw = view.getTopInventory().getSize(); raw < view.countSlots(); raw++) {
            ItemStack item = view.getItem(raw);
            if (item != null && item.getType() == material) return raw;
        }
        throw new AssertionError("Nenhum slot do inventario pessoal contem " + material);
    }

    private int countIn(PlayerMock player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) total += item.getAmount();
        }
        return total;
    }

    // ------------------------------------------------------------ ciclo de vida

    @Test
    void injetaLapisAoAbrirMesaNoHgWorld() {
        InventoryView view = openEnchantingTable(playerIn(hgWorld));

        ItemStack lapis = view.getTopInventory().getItem(LAPIS_SLOT);
        assertNotNull(lapis);
        assertEquals(Material.LAPIS_LAZULI, lapis.getType());
        assertEquals(64, lapis.getAmount());
    }

    @Test
    void naoInjetaLapisEmMesaForaDoHgWorld() {
        InventoryView view = openEnchantingTable(playerIn(lobby));

        assertNull(view.getTopInventory().getItem(LAPIS_SLOT));
    }

    @Test
    void naoSobrescreveItemQueJaEstavaNoSlotDoLapis() {
        PlayerMock player = playerIn(hgWorld);
        Inventory table = server.createInventory(null, InventoryType.ENCHANTING);
        table.setItem(LAPIS_SLOT, new ItemStack(Material.DIAMOND, 1));

        InventoryView view = player.openInventory(table);
        assertNotNull(view);
        server.getPluginManager().callEvent(new InventoryOpenEvent(view));

        ItemStack slot = view.getTopInventory().getItem(LAPIS_SLOT);
        assertNotNull(slot);
        assertEquals(Material.DIAMOND, slot.getType());
    }

    @Test
    void preservaLapisPessoalQueJaEstavaNaMesa() {
        PlayerMock player = playerIn(hgWorld);
        Inventory table = server.createInventory(null, InventoryType.ENCHANTING);
        table.setItem(LAPIS_SLOT, new ItemStack(Material.LAPIS_LAZULI, 5));
        InventoryView view = player.openInventory(table);
        assertNotNull(view);
        close(view);
        assertEquals(5, table.getItem(LAPIS_SLOT).getAmount());
    }

    @Test
    void removeLapisAoFecharParaNaoEscapar() {
        // O servidor devolve o conteudo da mesa ao jogador depois do close;
        // esvaziar o slot aqui e o que impede a duplicacao.
        InventoryView view = openEnchantingTable(playerIn(hgWorld));

        close(view);

        assertNull(view.getTopInventory().getItem(LAPIS_SLOT));
    }

    @Test
    void ciclosDeAbrirEFecharNaoAcumulamLapisNoJogador() {
        PlayerMock player = playerIn(hgWorld);

        for (int i = 0; i < 3; i++) {
            InventoryView view = openEnchantingTable(player);
            close(view);
            assertNull(view.getTopInventory().getItem(LAPIS_SLOT));
        }

        assertEquals(0, countIn(player, Material.LAPIS_LAZULI));
    }

    @Test
    void removeLapisAoDesconectarComMesaAberta() {
        PlayerMock player = playerIn(hgWorld);
        InventoryView view = openEnchantingTable(player);

        quit(player);

        assertNull(view.getTopInventory().getItem(LAPIS_SLOT));
    }

    @Test
    void fecharMesaForaDoHgWorldNaoLimpaLapisDoJogador() {
        PlayerMock player = playerIn(lobby);
        InventoryView view = openEnchantingTable(player);
        view.getTopInventory().setItem(LAPIS_SLOT, new ItemStack(Material.LAPIS_LAZULI, 5));

        close(view);

        ItemStack lapis = view.getTopInventory().getItem(LAPIS_SLOT);
        assertNotNull(lapis, "lapis do proprio jogador nao pode ser apagado fora do HG");
        assertEquals(5, lapis.getAmount());
    }

    // ----------------------------------------------------------------- cliques

    @Test
    void bloqueiaCliqueNoSlotDoLapis() {
        InventoryView view = openEnchantingTable(playerIn(hgWorld));

        assertTrue(click(view, LAPIS_SLOT, InventoryAction.PICKUP_ALL).isCancelled());
    }

    @Test
    void bloqueiaShiftClickNoSlotDoLapis() {
        InventoryView view = openEnchantingTable(playerIn(hgWorld));

        assertTrue(click(view, LAPIS_SLOT, InventoryAction.MOVE_TO_OTHER_INVENTORY).isCancelled());
    }

    @Test
    void bloqueiaHotbarSwapNoSlotDoLapis() {
        InventoryView view = openEnchantingTable(playerIn(hgWorld));

        InventoryClickEvent event = new InventoryClickEvent(
                view, InventoryType.SlotType.CRAFTING, LAPIS_SLOT,
                ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 0);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void naoBloqueiaSlotDoLapisEmMesaForaDoHgWorld() {
        InventoryView view = openEnchantingTable(playerIn(lobby));

        assertFalse(click(view, LAPIS_SLOT, InventoryAction.PICKUP_ALL).isCancelled());
    }

    @Test
    void bloqueiaShiftClickDeLapisDoInventarioPessoal() {
        PlayerMock player = playerIn(hgWorld);
        player.getInventory().addItem(new ItemStack(Material.LAPIS_LAZULI, 16));
        InventoryView view = openEnchantingTable(player);

        int raw = rawSlotOf(view, Material.LAPIS_LAZULI);

        assertTrue(click(view, raw, InventoryAction.MOVE_TO_OTHER_INVENTORY).isCancelled());
    }

    @Test
    void naoBloqueiaShiftClickDeItemEncantavel() {
        PlayerMock player = playerIn(hgWorld);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD, 1));
        InventoryView view = openEnchantingTable(player);

        int raw = rawSlotOf(view, Material.DIAMOND_SWORD);

        assertFalse(click(view, raw, InventoryAction.MOVE_TO_OTHER_INVENTORY).isCancelled());
    }

    @Test
    void naoBloqueiaCliqueComumNoInventarioPessoal() {
        PlayerMock player = playerIn(hgWorld);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD, 1));
        InventoryView view = openEnchantingTable(player);

        int raw = rawSlotOf(view, Material.DIAMOND_SWORD);

        assertFalse(click(view, raw, InventoryAction.PICKUP_ALL).isCancelled());
    }

    @Test
    void bloqueiaDuploCliqueQueRecolheLapisDaMesa() {
        PlayerMock player = playerIn(hgWorld);
        InventoryView view = openEnchantingTable(player);
        view.setCursor(new ItemStack(Material.LAPIS_LAZULI, 1));

        assertTrue(click(view, view.getTopInventory().getSize(), InventoryAction.COLLECT_TO_CURSOR).isCancelled());
    }

    @Test
    void naoBloqueiaDuploCliqueDeOutroItem() {
        PlayerMock player = playerIn(hgWorld);
        InventoryView view = openEnchantingTable(player);
        view.setCursor(new ItemStack(Material.DIAMOND, 1));

        assertFalse(click(view, view.getTopInventory().getSize(), InventoryAction.COLLECT_TO_CURSOR).isCancelled());
    }

    // ------------------------------------------------------------------- drags

    @Test
    void bloqueiaDragQueTocaOSlotDoLapis() {
        InventoryView view = openEnchantingTable(playerIn(hgWorld));
        ItemStack cursor = new ItemStack(Material.LAPIS_LAZULI, 4);

        assertTrue(drag(view, cursor, LAPIS_SLOT, view.getTopInventory().getSize()).isCancelled());
    }

    @Test
    void naoBloqueiaDragApenasNoInventarioPessoal() {
        InventoryView view = openEnchantingTable(playerIn(hgWorld));
        ItemStack cursor = new ItemStack(Material.LAPIS_LAZULI, 4);

        int top = view.getTopInventory().getSize();

        assertFalse(drag(view, cursor, top, top + 1).isCancelled());
    }

    @Test
    void naoBloqueiaDragEmMesaForaDoHgWorld() {
        InventoryView view = openEnchantingTable(playerIn(lobby));
        ItemStack cursor = new ItemStack(Material.LAPIS_LAZULI, 4);

        assertFalse(drag(view, cursor, LAPIS_SLOT).isCancelled());
    }
}
