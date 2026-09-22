package br.dev.sno0s.hgplugin.utils;

import br.dev.sno0s.hgplugin.GameState;
import br.dev.sno0s.hgplugin.items.KitSelector;
import br.dev.sno0s.hgplugin.items.PluginItems;
import br.dev.sno0s.hgplugin.items.PluginMenu;
import br.dev.sno0s.hgplugin.kits.KangarooKit;
import br.dev.sno0s.hgplugin.listeners.KitSelectorListener;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessagesTest {
    private ServerMock server;
    private JavaPlugin plugin;
    private File file;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        file = new File(plugin.getDataFolder(), "messages.yml");
        Messages.load(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Evita que uma tradução deste teste afete os outros testes do plugin.
        Files.writeString(file.toPath(), "{}\n");
        Messages.load(plugin);
        MockBukkit.unmock();
    }

    @Test
    void customPrefixPaletteAndNamedParametersReachThePlayer() throws Exception {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("server-name", "&bMeu HG");
        config.set("prefix", "&8<{server-name}&8> ");
        config.set("colors.success", "&2");
        config.set("kits.selected", "Selected {kit}!");
        config.save(file);
        Messages.load(plugin);
        var player = server.addPlayer();
        Messages.success(player, "kits.selected", "kit", "$1\\{server-name}&c");
        assertEquals("§8<§bMeu HG§8> §2Selected $1\\{server-name}&c!", player.nextMessage());
        assertEquals("<Meu HG> Selected Test!", Messages.log("kits.selected", "kit", "Test"));
    }

    @Test
    void missingKeysAreAddedWithoutOverwritingCustomizationOrBlankMessages() throws Exception {
        Files.writeString(file.toPath(), "server-name: Custom\njoin:\n  announcement: ''\n");
        Messages.load(plugin);
        var saved = YamlConfiguration.loadConfiguration(file);
        assertEquals("Custom", saved.getString("server-name"));
        assertTrue(saved.isString("crafty.timeout"));
        assertEquals("", saved.getString("join.announcement"));
        var player = server.addPlayer();
        Messages.broadcastInfo("join.announcement", "player", player.getName());
        assertNull(player.nextMessage());
    }

    @Test
    void firstLoadMigratesLegacyTagAndColorsOnlyOnce() throws Exception {
        Files.delete(file.toPath());
        plugin.getConfig().set("HGconfigs.server-name", "§e[Meu servidor]");
        plugin.getConfig().set("HGconfigs.colors.error", "&4");
        Messages.load(plugin);
        assertEquals("§e[Meu servidor] ", Messages.text("prefix"));
        assertEquals("§4", Messages.text("colors.error"));
        plugin.getConfig().set("HGconfigs.server-name", "Não deve substituir");
        Messages.load(plugin);
        assertEquals("§e[Meu servidor] ", Messages.text("prefix"));
    }

    @Test
    void invalidYamlIsPreservedAndDoesNotReplaceTheWorkingCatalog() throws Exception {
        String prefix = Messages.text("prefix");
        String invalid = "prefix: [unterminated\n";
        Files.writeString(file.toPath(), invalid);
        assertThrows(IllegalStateException.class, () -> Messages.load(plugin));
        assertEquals(invalid, Files.readString(file.toPath()));
        assertEquals(prefix, Messages.text("prefix"));
    }

    @Test
    void renamedSelectorAndKitStillOpenAndSelectUsingPersistentIdentifiers() throws Exception {
        var config = YamlConfiguration.loadConfiguration(file);
        config.set("items.kit-selector.name", "&dChoose a class");
        config.set("items.kit-selector.lore", List.of("&7Custom description"));
        config.set("menus.kits.title", "Completely different title");
        config.set("kits.kangaroo.name", "Jump master");
        config.save(file);
        Messages.load(plugin);
        GameState.init();
        var player = server.addPlayer();
        GameState.getInstance().registerPlayer(player);
        var selector = KitSelector.create();
        assertEquals("§dChoose a class", selector.getItemMeta().getDisplayName());
        assertEquals(List.of("§7Custom description"), selector.getItemMeta().getLore());
        assertTrue(PluginItems.is(selector, "kit-selector"));
        var listener = new KitSelectorListener();
        listener.onKitSelectorUse(new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR,
                selector, null, null, EquipmentSlot.HAND));
        assertEquals("Completely different title", player.getOpenInventory().getTitle());
        assertInstanceOf(PluginMenu.class, player.getOpenInventory().getTopInventory().getHolder());
        var icon = player.getOpenInventory().getTopInventory().getItem(10);
        assertEquals("§eJump master", icon.getItemMeta().getDisplayName());
        var click = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                10, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        listener.onKitClick(click);
        assertTrue(click.isCancelled());
        assertEquals("Kangaroo", GameState.getInstance().getPlayer(player.getUniqueId()).getSelectedKit());
        assertTrue(player.nextMessage().contains("Jump master"));
        assertEquals("Kangaroo", PluginItems.id(new KangarooKit().getIcon()));
        assertEquals("Kangaroo", br.dev.sno0s.hgplugin.kits.KitRegistry.getByName("Jump master").getName());
    }
}
