package br.dev.sno0s.hgplugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

class ConfigManagerTest {

    private final List<String> warnings = new ArrayList<>();

    /** Plugin de teste com pasta de dados própria, para exercitar o feast.yml. */
    private Hgplugin pluginWith(YamlConfiguration config, Path folder) {
        Hgplugin plugin = mock(Hgplugin.class);
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override public void publish(LogRecord record) { warnings.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        return plugin;
    }

    private YamlConfiguration feastFile(Path folder) {
        return YamlConfiguration.loadConfiguration(new File(folder.toFile(), "feast.yml"));
    }

    private static Map<String, Object> lootEntry(String material, int amount) {
        return Map.of("material", material, "amount", amount);
    }

    @Test
    void movesLegacyFeastLootIntoFeastFileOnceAndKeepsTheServerList(@TempDir Path folder) {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> custom = List.of(lootEntry("NETHERITE_SWORD", 1), lootEntry("COOKIE", 9));
        config.set(ConfigManager.LEGACY_LOOT, custom);

        ConfigManager manager = new ConfigManager(pluginWith(config, folder));

        assertFalse(config.isSet(ConfigManager.LEGACY_LOOT), "o loot sai do config.yml");
        assertEquals(2, manager.getFeastLoot().size());
        assertEquals("NETHERITE_SWORD", manager.getFeastLoot().getFirst().get("material"));
        assertEquals(custom, feastFile(folder).getList("loot"));

        // Segunda inicialização não tem nada a migrar e mantém a lista do servidor.
        ConfigManager again = new ConfigManager(pluginWith(config, folder));
        assertEquals(custom, feastFile(folder).getList("loot"));
        assertEquals(2, again.getFeastLoot().size());
    }

    @Test
    void keepsFeastLootAlreadyCustomizedInTheNewFile(@TempDir Path folder) throws Exception {
        YamlConfiguration feast = new YamlConfiguration();
        List<Map<String, Object>> chosen = List.of(lootEntry("GOLDEN_APPLE", 4));
        feast.set("loot", chosen);
        feast.save(new File(folder.toFile(), "feast.yml"));

        YamlConfiguration config = new YamlConfiguration();
        config.set(ConfigManager.LEGACY_LOOT, List.of(lootEntry("STONE", 64)));
        ConfigManager manager = new ConfigManager(pluginWith(config, folder));

        assertEquals(chosen, feastFile(folder).getList("loot"));
        assertEquals("GOLDEN_APPLE", manager.getFeastLoot().getFirst().get("material"));
        assertFalse(config.isSet(ConfigManager.LEGACY_LOOT));
    }

    @Test
    void newInstallGetsTheBundledLoot(@TempDir Path folder) {
        ConfigManager manager = new ConfigManager(pluginWith(new YamlConfiguration(), folder));

        assertFalse(manager.getFeastLoot().isEmpty(), "instalação nova recebe o loot embutido");
        assertTrue(feastFile(folder).isList("loot"));
        assertTrue(warnings.isEmpty(), "loot padrão não gera aviso");
    }

    @Test
    void reportsMalformedLootInsteadOfReplacingIt(@TempDir Path folder) throws Exception {
        File file = new File(folder.toFile(), "feast.yml");
        Files.writeString(file.toPath(), "loot: \"um item só\"\n");

        ConfigManager manager = new ConfigManager(pluginWith(new YamlConfiguration(), folder));

        assertTrue(manager.getFeastLoot().isEmpty());
        assertTrue(warnings.stream().anyMatch(message -> message.contains("loot")), warnings.toString());
        assertEquals("um item só", feastFile(folder).getString("loot"), "o valor inválido é preservado");
    }

    @Test
    void skipsUnusableLootEntriesAndKeepsTheValidOnes(@TempDir Path folder) throws Exception {
        YamlConfiguration feast = new YamlConfiguration();
        feast.set("loot", List.of(lootEntry("TNT", 2), "COBWEB"));
        feast.save(new File(folder.toFile(), "feast.yml"));

        ConfigManager manager = new ConfigManager(pluginWith(new YamlConfiguration(), folder));

        assertEquals(1, manager.getFeastLoot().size());
        assertEquals("TNT", manager.getFeastLoot().getFirst().get("material"));
        assertTrue(warnings.stream().anyMatch(message -> message.contains("COBWEB")), warnings.toString());
    }

    @Test
    void startupOrderPreservesCustomLootAndCreatesBackup(@TempDir Path folder) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        var custom = List.of(Map.of("material", "ENCHANTED_BOOK", "amount", 2,
                "enchantments", Map.of("SHARPNESS", 3)));
        config.set(ConfigManager.LEGACY_LOOT, custom);
        File original = folder.resolve("config.yml").toFile();
        config.save(original);
        String before = Files.readString(original.toPath());
        Hgplugin plugin = pluginWith(config, folder);
        doAnswer(invocation -> { config.save(original); return null; }).when(plugin).saveConfig();

        // Mesma ordem de Hgplugin.onEnable: Messages primeiro, ConfigManager depois.
        br.dev.sno0s.hgplugin.utils.Messages.load(plugin);
        assertFalse(Files.exists(folder.resolve("feast.yml")));
        var manager = new ConfigManager(plugin);
        assertEquals(custom, manager.getFeastLoot());
        try (var paths = Files.list(folder)) {
            var backups = paths.filter(path -> path.getFileName().toString().startsWith("config.yml.")
                    && path.toString().endsWith(".bak")).toList();
            assertEquals(1, backups.size());
            assertEquals(before, Files.readString(backups.getFirst()));
        }
        var fileTime = Files.getLastModifiedTime(folder.resolve("feast.yml"));
        config.set(ConfigManager.LEGACY_LOOT, null);
        new ConfigManager(plugin);
        assertEquals(fileTime, Files.getLastModifiedTime(folder.resolve("feast.yml")));
        assertEquals(custom, feastFile(folder).getList("loot"));
    }

    @Test
    void preservesAnIntentionallyEmptyLegacyFeast(@TempDir Path folder) {
        var config = new YamlConfiguration();
        config.set(ConfigManager.LEGACY_LOOT, List.of());
        Hgplugin plugin = pluginWith(config, folder);
        br.dev.sno0s.hgplugin.utils.Messages.load(plugin);
        assertTrue(new ConfigManager(plugin).getFeastLoot().isEmpty());
        assertEquals(List.of(), feastFile(folder).getList("loot"));
    }

    @Test
    void malformedLegacyLootIsNotReplacedWithDefaults(@TempDir Path folder) {
        var config = new YamlConfiguration();
        config.set(ConfigManager.LEGACY_LOOT, "valor inválido");
        var manager = new ConfigManager(pluginWith(config, folder));
        assertTrue(manager.getFeastLoot().isEmpty());
        assertEquals("valor inválido", feastFile(folder).get("loot"));
        assertFalse(warnings.isEmpty());
    }

    @Test
    void invalidConfigOnDiskAbortsBeforeMigration(@TempDir Path folder) throws Exception {
        String invalid = "HGconfigs: [";
        Files.writeString(folder.resolve("config.yml"), invalid);
        assertThrows(IllegalStateException.class,
                () -> new ConfigManager(pluginWith(new YamlConfiguration(), folder)));
        assertEquals(invalid, Files.readString(folder.resolve("config.yml")));
        assertFalse(Files.exists(folder.resolve("feast.yml")));
    }

    @Test
    void upgradesOldPresetOnceEvenWhenNewDefaultsArePresent() {
        YamlConfiguration config = new YamlConfiguration();
        config.addDefault("HGconfigs.terrain.generator-version", 2);
        config.set("HGconfigs.terrain.biome-frequency", 0.003);
        config.set("HGconfigs.terrain.plains-weight", 0.2);
        assertTrue(ConfigManager.migrateTerrainDefaults(config));
        assertEquals(0.008, config.getDouble("HGconfigs.terrain.biome-frequency"));
        assertEquals(-0.15, config.getDouble("HGconfigs.terrain.plains-weight"));
        assertEquals(12, config.getInt("HGconfigs.cocoa-density"));
        assertEquals(750, config.getInt("HGconfigs.world-size"));
        assertEquals(.25, config.getDouble("HGconfigs.mob-spawn-multiplier"));
        config.set("HGconfigs.terrain.plains-weight", 0.2);
        assertFalse(ConfigManager.migrateTerrainDefaults(config));
        assertEquals(0.2, config.getDouble("HGconfigs.terrain.plains-weight"));
    }

    @Test
    void addsArenaSettingsToVersionTwoAndPreservesCustomValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("HGconfigs.terrain.generator-version", 2);
        config.set("HGconfigs.terrain.plains-weight", -.15);
        assertTrue(ConfigManager.migrateTerrainDefaults(config));
        assertEquals(750, config.getInt("HGconfigs.world-size"));
        assertEquals(-.15, config.getDouble("HGconfigs.terrain.plains-weight"));
        assertEquals(3, config.getInt("HGconfigs.terrain.generator-version"));
        config.set("HGconfigs.world-size", 1200);
        config.set("HGconfigs.mob-spawn-multiplier", .5);
        assertFalse(ConfigManager.migrateTerrainDefaults(config));
        assertEquals(1200, config.getInt("HGconfigs.world-size"));
        assertEquals(.5, config.getDouble("HGconfigs.mob-spawn-multiplier"));
    }

    @Test
    void terrainFallbackRetainsConfiguredArenaSize() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("HGconfigs.world-size", 1200);
        config.set("HGconfigs.terrain.height-variation", -1);
        Hgplugin plugin = mock(Hgplugin.class);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        ConfigManager manager = new ConfigManager(plugin);
        assertEquals(1200, manager.getTerrainProfile().worldSize());
        config.set("HGconfigs.world-size", 751);
        assertEquals(750, manager.getWorldSize());
        config.set("HGconfigs.world-size", 0);
        assertEquals(750, manager.getWorldSize());
    }

    @Test
    void keepsCustomBiomeSettings() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("HGconfigs.terrain.biome-frequency", 0.01);
        config.set("HGconfigs.terrain.plains-weight", 0.4);
        assertTrue(ConfigManager.migrateTerrainDefaults(config));
        assertEquals(0.01, config.getDouble("HGconfigs.terrain.biome-frequency"));
        assertEquals(0.4, config.getDouble("HGconfigs.terrain.plains-weight"));
    }
}
