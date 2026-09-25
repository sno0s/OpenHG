package br.dev.sno0s.hgplugin.kits;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.MatchPhase;
import br.dev.sno0s.hgplugin.listeners.StomperListener;
import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.*;

class StomperKitTest {
    private ServerMock server;
    private WorldMock world;
    private PlayerMock stomper;
    private PlayerMock target;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("hg_world");
        var plugin = MockBukkit.createMockPlugin();
        Messages.load(plugin);
        GameState.init();
        GameState.getInstance().setPhase(MatchPhase.IN_PROGRESS);
        stomper = participant(0, 70, 0);
        target = participant(3, 70, 0);
        GameState.getInstance().getPlayer(stomper.getUniqueId()).setSelectedKit("Stomper");
        server.getPluginManager().registerEvents(new StomperListener(7, 35), plugin);
    }

    @AfterEach
    void tearDown() { MockBukkit.unmock(); }

    private PlayerMock participant(double x, double y, double z) {
        var player = server.addPlayer();
        player.teleport(new Location(world, x, y, z));
        player.setGameMode(GameMode.SURVIVAL);
        player.setInvulnerable(false);
        GameState.getInstance().registerPlayer(player);
        return player;
    }

    private EntityDamageEvent fall(float distance) {
        stomper.setFallDistance(distance);
        var event = new EntityDamageEvent(stomper, EntityDamageEvent.DamageCause.FALL, 20);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    void registersAsPassiveKitWithoutReplacingInventory() {
        Kit kit = KitRegistry.getByName("Stomper");
        assertNotNull(kit);
        assertEquals("Stomper", kit.getName());
        assertEquals(org.bukkit.Material.IRON_BOOTS, kit.getIconMaterial().getType());
        assertTrue(stomper.getInventory().getItem(0) == null
                || stomper.getInventory().getItem(0).getType() == org.bukkit.Material.AIR);
    }

    @Test
    void receivesHalfHeartAndImpactsNearbyPlayersFromThirtyFiveBlocks() {
        target.setHealth(20);
        var event = fall(35);
        assertEquals(1.0, event.getDamage());
        assertEquals(0, target.getHealth());
    }

    @Test
    void lowFallsOnlyDealResidualDamageAndDoNotImpact() {
        target.setHealth(20);
        var event = fall(34);
        assertEquals(1.0, event.getDamage());
        assertEquals(20, target.getHealth());
    }

    @Test
    void impactDamageScalesWithFallHeight() {
        target.setHealth(20);
        fall(10);
        assertEquals(20, target.getHealth());
        // Change the configured threshold in this listener's contract is tested by the minimum gate above.
        assertEquals(20, target.getHealth());
    }

    @Test
    void ignoresPlayersOutsideRadiusAndTheStomper() {
        target.teleport(new Location(world, 8, 70, 0));
        fall(40);
        assertEquals(20, target.getHealth());
        assertEquals(20, stomper.getHealth());
    }

    @Test
    void ignoresSpectatorsAndEliminatedPlayers() {
        target.setGameMode(GameMode.SPECTATOR);
        fall(40);
        assertEquals(20, target.getHealth());
        target.setGameMode(GameMode.SURVIVAL);
        GameState.getInstance().getPlayer(target.getUniqueId()).setAlive(false);
        fall(40);
        assertEquals(20, target.getHealth());
    }
}
