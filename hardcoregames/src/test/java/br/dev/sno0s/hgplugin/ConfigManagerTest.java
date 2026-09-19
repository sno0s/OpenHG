package br.dev.sno0s.hgplugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        config.set("HGconfigs.terrain.plains-weight", 0.2);
        assertFalse(ConfigManager.migrateTerrainDefaults(config));
        assertEquals(0.2, config.getDouble("HGconfigs.terrain.plains-weight"));
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
