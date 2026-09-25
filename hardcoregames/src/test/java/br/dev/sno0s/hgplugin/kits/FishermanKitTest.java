package br.dev.sno0s.hgplugin.kits;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.items.PluginItems;
import br.dev.sno0s.hgplugin.listeners.FishermanListener;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FishermanKitTest {
    private ServerMock server;
    private WorldMock world;
    private PlayerMock fisherman;
    private PlayerMock target;
    private FishHook hook;
    private Kit kit;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("hg_world");
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(750);
        var plugin = MockBukkit.createMockPlugin();
        Messages.load(plugin);
        GameState.init();
        GameState.getInstance().setPhase(MatchPhase.IN_PROGRESS);
        fisherman = participant(10);
        target = participant(20);
        GameState.getInstance().getPlayer(fisherman.getUniqueId()).setSelectedKit("Fisherman");
        kit = KitRegistry.getByName("Fisherman");
        assertNotNull(kit, "Fisherman must be registered");
        kit.apply(fisherman);
        server.getPluginManager().registerEvents(new FishermanListener(), plugin);
        hook = mock(FishHook.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerMock participant(double x) {
        var player = server.addPlayer();
        player.teleport(new Location(world, x, 70, 0));
        player.setGameMode(GameMode.SURVIVAL);
        player.setInvulnerable(false);
        GameState.getInstance().registerPlayer(player);
        return player;
    }

    private PlayerFishEvent reel() {
        var event = new PlayerFishEvent(fisherman, target, hook,
                EquipmentSlot.HAND, PlayerFishEvent.State.REEL_IN);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private void assertBlocked() {
        Location before = target.getLocation();
        assertTrue(reel().isCancelled(), "Do not fall back to vanilla pulling on a denied kit use");
        assertEquals(before, target.getLocation());
        verify(hook).remove();
    }

    @Test
    void kitItemAndMenuShareCatalogButHaveSeparateIdentity() {
        ItemStack rod = fisherman.getInventory().getItem(0);
        assertEquals(Material.FISHING_ROD, rod.getType());
        assertTrue(PluginItems.is(rod, "fisherman-rod"));
        assertEquals(Messages.text("items.fisherman.name"), rod.getItemMeta().getDisplayName());
        assertEquals(Messages.lines("items.fisherman.lore"), rod.getItemMeta().getLore());
        // Componentes normalizam códigos de cor redundantes do template do menu.
        assertEquals(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .deserialize(Messages.text("menus.kits.icon-name", "kit", kit.getDisplayName())),
                kit.getIcon().getItemMeta().displayName());
        assertEquals(rod.getItemMeta().getLore(), kit.getIcon().getItemMeta().getLore());
        assertTrue(PluginItems.is(kit.getIcon(), "Fisherman"));
    }

    @Test
    void reelTeleportsCaughtPlayerWithoutDamageOrCooldown() {
        double health = target.getHealth();
        assertTrue(reel().isCancelled());
        assertEquals(fisherman.getLocation(), target.getLocation());
        assertEquals(health, target.getHealth());
        target.teleport(new Location(world, 30, 70, 0));
        reel();
        assertEquals(fisherman.getLocation(), target.getLocation());
    }

    @Test
    void caughtEntityStateAlsoTeleportsWhenPaperEmitsItOnRetrieval() {
        var event = new PlayerFishEvent(fisherman, target, hook,
                EquipmentSlot.HAND, PlayerFishEvent.State.CAUGHT_ENTITY);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
        assertEquals(fisherman.getLocation(), target.getLocation());
    }

    @Test
    void offhandRodWorks() {
        fisherman.getInventory().setItemInOffHand(fisherman.getInventory().getItemInMainHand());
        fisherman.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        server.getPluginManager().callEvent(new PlayerFishEvent(fisherman, target, hook,
                EquipmentSlot.OFF_HAND, PlayerFishEvent.State.REEL_IN));
        assertEquals(fisherman.getLocation(), target.getLocation());
    }

    @Test
    void ordinaryRodDoesNotActivateKit() {
        fisherman.getInventory().setItemInMainHand(new ItemStack(Material.FISHING_ROD));
        Location before = target.getLocation();
        assertFalse(reel().isCancelled());
        assertEquals(before, target.getLocation());
        verifyNoInteractions(hook);
    }

    @Test
    void renamingRodDoesNotChangeItsIdentity() {
        var rod = fisherman.getInventory().getItemInMainHand();
        var meta = rod.getItemMeta();
        meta.setDisplayName("Vara traduzida");
        rod.setItemMeta(meta);
        fisherman.getInventory().setItemInMainHand(rod);
        reel();
        assertEquals(fisherman.getLocation(), target.getLocation());
    }

    @Test
    void kitRodCannotBeDropped() {
        server.getPluginManager().registerEvents(new br.dev.sno0s.hgplugin.listeners.OnDrop(),
                MockBukkit.createMockPlugin());
        var item = mock(org.bukkit.entity.Item.class);
        when(item.getItemStack()).thenReturn(fisherman.getInventory().getItemInMainHand());
        var event = new org.bukkit.event.player.PlayerDropItemEvent(fisherman, item);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void nonFishermanCannotUseMarkedRod() {
        GameState.getInstance().getPlayer(fisherman.getUniqueId()).setSelectedKit("Kangaroo");
        assertBlocked();
    }

    @Test
    void blocksWaitingCountdownAndEndedMatches() {
        for (var phase : new MatchPhase[]{MatchPhase.WAITING, MatchPhase.COUNTDOWN, MatchPhase.ENDED}) {
            GameState.getInstance().setPhase(phase);
            reset(hook);
            assertBlocked();
        }
    }

    @Test
    void blocksInvulnerableTargetAndFisherman() {
        target.setInvulnerable(true);
        assertBlocked();
        target.setInvulnerable(false);
        fisherman.setInvulnerable(true);
        reset(hook);
        assertBlocked();
    }

    @Test
    void blocksSpectatorsAndEliminatedPlayersOnBothSides() {
        for (var player : new PlayerMock[]{fisherman, target}) {
            player.setGameMode(GameMode.SPECTATOR);
            reset(hook);
            assertBlocked();
            player.setGameMode(GameMode.SURVIVAL);
            var data = GameState.getInstance().getPlayer(player.getUniqueId());
            data.setAlive(false);
            reset(hook);
            assertBlocked();
            data.setAlive(true);
        }
    }

    @Test
    void blocksUnregisteredTarget() {
        GameState.init();
        GameState.getInstance().setPhase(MatchPhase.IN_PROGRESS);
        GameState.getInstance().registerPlayer(fisherman);
        GameState.getInstance().getPlayer(fisherman.getUniqueId()).setSelectedKit("Fisherman");
        assertBlocked();
    }

    @Test
    void blocksOtherWorldsAndOutsideBorder() {
        target.teleport(server.addSimpleWorld("lobby").getSpawnLocation());
        assertBlocked();
        target.teleport(new Location(world, 20, 70, 0));
        // MockBukkit 4.116.1 usa size como raio em isInside; 800 fica fora
        // tanto da simulação quanto da borda real de 750 blocos de lado.
        fisherman.teleport(new Location(world, 800, 70, 0));
        assertFalse(world.getWorldBorder().isInside(fisherman.getLocation()));
        reset(hook);
        assertBlocked();
        fisherman.teleport(new Location(world, 10, 70, 0));
        target.teleport(new Location(world, 800, 70, 0));
        assertFalse(world.getWorldBorder().isInside(target.getLocation()));
        reset(hook);
        assertBlocked();
    }

    @Test
    void respectsCancelledFishingEvent() {
        Location before = target.getLocation();
        var event = new PlayerFishEvent(fisherman, target, hook,
                EquipmentSlot.HAND, PlayerFishEvent.State.REEL_IN);
        event.setCancelled(true);
        server.getPluginManager().callEvent(event);
        assertEquals(before, target.getLocation());
        verifyNoInteractions(hook);
    }

    @Test
    void respectsCancelledTeleport() {
        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onTeleport(PlayerTeleportEvent event) { event.setCancelled(true); }
        }, MockBukkit.createMockPlugin());
        Location before = target.getLocation();
        assertTrue(reel().isCancelled());
        assertEquals(before, target.getLocation());
    }

    @Test
    void castingAndFishingNonPlayersRemainNormal() {
        Location before = target.getLocation();
        var cast = new PlayerFishEvent(fisherman, null, hook,
                EquipmentSlot.HAND, PlayerFishEvent.State.FISHING);
        server.getPluginManager().callEvent(cast);
        var item = new PlayerFishEvent(fisherman, mock(org.bukkit.entity.Item.class), hook,
                EquipmentSlot.HAND, PlayerFishEvent.State.REEL_IN);
        server.getPluginManager().callEvent(item);
        assertFalse(cast.isCancelled());
        assertFalse(item.isCancelled());
        assertEquals(before, target.getLocation());
    }
}
