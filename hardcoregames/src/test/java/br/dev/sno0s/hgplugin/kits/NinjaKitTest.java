package br.dev.sno0s.hgplugin.kits;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.listeners.NinjaListener;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.*;

class NinjaKitTest {
    private ServerMock server;
    private WorldMock world;
    private PlayerMock ninja;
    private PlayerMock target;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("hg_world");
        var plugin = MockBukkit.createMockPlugin();
        Messages.load(plugin);
        GameState.init();
        GameState.getInstance().setPhase(MatchPhase.IN_PROGRESS);
        ninja = participant(0);
        target = participant(4);
        GameState.getInstance().getPlayer(ninja.getUniqueId()).setSelectedKit("Ninja");
        server.getPluginManager().registerEvents(new NinjaListener(15), plugin);
    }

    @AfterEach
    void tearDown() { MockBukkit.unmock(); }

    private PlayerMock participant(double x) {
        var player = server.addPlayer();
        player.teleport(new Location(world, x, 70, 0));
        player.setGameMode(GameMode.SURVIVAL);
        player.setInvulnerable(false);
        GameState.getInstance().registerPlayer(player);
        return player;
    }

    private void hitTarget() {
        server.getPluginManager().callEvent(new EntityDamageByEntityEvent(ninja, target,
                EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK, 1));
    }

    @Test
    void registersAsPassiveNinjaWithEmeraldIcon() {
        Kit kit = KitRegistry.getByName("Ninja");
        assertNotNull(kit);
        assertEquals(Material.EMERALD, kit.getIconMaterial().getType());
        kit.apply(ninja);
        assertTrue(ninja.getInventory().getItem(0) == null
                || ninja.getInventory().getItem(0).getType() == Material.AIR);
    }

    @Test
    void sneakTeleportsToLastHitTarget() {
        hitTarget();
        target.teleport(new Location(world, 20, 70, 0));
        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(ninja, true));
        assertEquals(target.getLocation(), ninja.getLocation());
    }

    @Test
    void remembersOnlyTheMostRecentTarget() {
        hitTarget();
        var second = participant(6);
        server.getPluginManager().callEvent(new EntityDamageByEntityEvent(ninja, second,
                EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK, 1));
        second.teleport(new Location(world, 18, 70, 0));
        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(ninja, true));
        assertEquals(second.getLocation(), ninja.getLocation());
    }

    @Test
    void cooldownBlocksSecondTeleport() {
        hitTarget();
        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(ninja, true));
        target.teleport(new Location(world, 25, 70, 0));
        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(ninja, true));
        assertNotEquals(target.getLocation(), ninja.getLocation());
    }

    @Test
    void ignoresTargetBeyondFiftyBlocks() {
        hitTarget();
        target.teleport(new Location(world, 51, 70, 0));
        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(ninja, true));
        assertNotEquals(target.getLocation(), ninja.getLocation());
    }

    @Test
    void sneakWithoutHitDoesNothing() {
        Location before = ninja.getLocation();
        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(ninja, true));
        assertEquals(before, ninja.getLocation());
    }

    @Test
    void onlyRisingSneakTriggersAbility() {
        hitTarget();
        server.getPluginManager().callEvent(new PlayerToggleSneakEvent(ninja, false));
        assertNotEquals(target.getLocation(), ninja.getLocation());
    }
}
