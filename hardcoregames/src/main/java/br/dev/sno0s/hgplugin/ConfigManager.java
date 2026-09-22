package br.dev.sno0s.hgplugin;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import br.dev.sno0s.hgplugin.worldgeneration.TerrainProfile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final Hgplugin plugin;

    public ConfigManager(Hgplugin plugin) {
        this.plugin = plugin;
        boolean changed = mergeBundledDefaults(plugin.getConfig());
        if (migrateTerrainDefaults(plugin.getConfig())) {
            changed = true;
        }
        if (changed) {
            plugin.saveConfig();
        }
    }

    /** Adiciona novas opções do config.yml sem apagar valores definidos pelo servidor. */
    private boolean mergeBundledDefaults(FileConfiguration config) {
        var resource = plugin.getResource("config.yml");
        // Test doubles may not expose bundled resources; the server JAR always does.
        if (resource == null) return false;
        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8));
        boolean changed = false;
        for (String key : bundled.getKeys(true)) {
            Object value = bundled.get(key);
            if (value != null && !bundled.isConfigurationSection(key) && !config.isSet(key)) {
                config.set(key, value);
                changed = true;
            }
        }
        return changed;
    }

    // Migra somente valores dos presets anteriores e preserva configurações personalizadas.
    static boolean migrateTerrainDefaults(FileConfiguration config) {
        boolean changed = false;
        if (!config.isSet("HGconfigs.cocoa-density")) {
            config.set("HGconfigs.cocoa-density", 12);
            changed = true;
        }
        if (!config.isSet("HGconfigs.world-size")) {
            config.set("HGconfigs.world-size", 750);
            changed = true;
        }
        if (!config.isSet("HGconfigs.mob-spawn-multiplier")) {
            config.set("HGconfigs.mob-spawn-multiplier", 0.25);
            changed = true;
        }
        String path = "HGconfigs.terrain.";
        int version = config.isSet(path + "generator-version") ? config.getInt(path + "generator-version") : 0;
        if (version >= 3) return changed;
        if (version < 2) {
            if (config.isSet(path + "biome-frequency") && config.getDouble(path + "biome-frequency") == 0.003) {
                config.set(path + "biome-frequency", 0.008);
            }
            if (config.isSet(path + "plains-weight") && config.getDouble(path + "plains-weight") == 0.2) {
                config.set(path + "plains-weight", -0.15);
            }
        }
        config.set(path + "generator-version", 3);
        return true;
    }

    public double getSoupHeal() {
        return plugin.getConfig().getDouble("HGconfigs.soup-heal", 6.0);
    }

    public int getSoupFood() {
        return plugin.getConfig().getInt("HGconfigs.soup-food", 7);
    }

    public int getMushroomDensity() {
        return plugin.getConfig().getInt("HGconfigs.mushroom-density", 40);
    }

    public int getCocoaDensity() {
        return Math.clamp(plugin.getConfig().getInt("HGconfigs.cocoa-density", 12), 0, 256);
    }

    public int getWorldSize() {
        int size = plugin.getConfig().getInt("HGconfigs.world-size", 750);
        if (size >= 256 && size <= 10000 && size % 2 == 0) return size;
        plugin.getLogger().warning(Messages.log("console.config-manager.invalid-world-size"));
        return 750;
    }

    public double getMobSpawnMultiplier() {
        double multiplier = plugin.getConfig().getDouble("HGconfigs.mob-spawn-multiplier", 0.25);
        return Double.isFinite(multiplier) && multiplier >= 0 && multiplier <= 1 ? multiplier : 0.25;
    }

    public String getCraftyUrl() {
        return plugin.getConfig().getString("HGconfigs.crafty.url", "https://10.170.184.252:8111");
    }

    public String getCraftyApiKey() {
        return plugin.getConfig().getString("HGconfigs.crafty.api-key", "");
    }

    public String getCraftyServerId() {
        return plugin.getConfig().getString("HGconfigs.crafty.server-id", "");
    }

    public int getCraftyRestartDelay() {
        return plugin.getConfig().getInt("HGconfigs.crafty.restart-delay", 15);
    }

    public int getKangarooHitCooldown() {
        return plugin.getConfig().getInt("HGconfigs.kangaroo.hit-cooldown", 3);
    }

    public double getBiomeFrequency() {
        return plugin.getConfig().getDouble("HGconfigs.terrain.biome-frequency", 0.008);
    }

    public double getBiomePlainsWeight() {
        return plugin.getConfig().getDouble("HGconfigs.terrain.plains-weight", -0.15);
    }

    public double getBiomeDarkForestWeight() {
        return plugin.getConfig().getDouble("HGconfigs.terrain.dark-forest-weight", -0.3);
    }

    public int getTreeDensity() {
        return plugin.getConfig().getInt("HGconfigs.tree-density", 1);
    }

    public int getWallHeight() {
        return Math.clamp(plugin.getConfig().getInt("HGconfigs.wall.height", 50), 6, 50);
    }

    public TerrainProfile getTerrainProfile() {
        FileConfiguration cfg = plugin.getConfig();
        try {
            return new TerrainProfile(
                    cfg.getInt("HGconfigs.terrain.base-height", 68),
                    cfg.getDouble("HGconfigs.terrain.height-variation", 12),
                    cfg.getDouble("HGconfigs.terrain.hill-frequency", 0.006),
                    getBiomeFrequency(), getBiomePlainsWeight(), getBiomeDarkForestWeight(), getWorldSize());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(Messages.log("console.config-manager.invalid-terrain", "detail", e.getMessage()));
            return new TerrainProfile(68, 12, 0.006, 0.008, -0.15, -0.3, getWorldSize());
        }
    }

    /** Acesso direto ao FileConfiguration para usos avançados (ex: feast-loot list). */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
