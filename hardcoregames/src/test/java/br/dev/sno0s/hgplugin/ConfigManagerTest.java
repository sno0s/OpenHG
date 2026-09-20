package br.dev.sno0s.hgplugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.logging.Logger;

class ConfigManagerTest {
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
