package br.dev.sno0s.hgplugin;

import org.bukkit.configuration.file.FileConfiguration;
import br.dev.sno0s.hgplugin.worldgeneration.TerrainProfile;

public class ConfigManager {

    private final Hgplugin plugin;

    public ConfigManager(Hgplugin plugin) {
        this.plugin = plugin;
        if (migrateTerrainDefaults(plugin.getConfig())) {
            plugin.saveConfig();
        }
    }

    /** Upgrade only the old preset values; preserve explicitly customized terrain settings. */
    static boolean migrateTerrainDefaults(FileConfiguration config) {
        String path = "HGconfigs.terrain.";
        if (config.isSet(path + "generator-version")) return false;
        if (config.isSet(path + "biome-frequency") && config.getDouble(path + "biome-frequency") == 0.003) {
            config.set(path + "biome-frequency", 0.008);
        }
        if (config.isSet(path + "plains-weight") && config.getDouble(path + "plains-weight") == 0.2) {
            config.set(path + "plains-weight", -0.15);
        }
        config.set(path + "generator-version", 2);
        return true;
    }

    public String getServerName() {
        return plugin.getConfig().getString("HGconfigs.server-name", "HardcoreGames");
    }

    public String getMsgColor() {
        return plugin.getConfig().getString("HGconfigs.colors.msg", "§f");
    }

    public String getHighlightColor() {
        return plugin.getConfig().getString("HGconfigs.colors.highlight", "§e");
    }

    public String getErrorColor() {
        return plugin.getConfig().getString("HGconfigs.colors.error", "§c");
    }

    public String getSuccessColor() {
        return plugin.getConfig().getString("HGconfigs.colors.success", "§a");
    }

    public String getBroadcastColor() {
        return plugin.getConfig().getString("HGconfigs.colors.broadcast", "§6");
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
        return Math.clamp(plugin.getConfig().getInt("HGconfigs.wall.height", 10), 6, 24);
    }

    public TerrainProfile getTerrainProfile() {
        FileConfiguration cfg = plugin.getConfig();
        try {
            return new TerrainProfile(
                    cfg.getInt("HGconfigs.terrain.base-height", 68),
                    cfg.getDouble("HGconfigs.terrain.height-variation", 12),
                    cfg.getDouble("HGconfigs.terrain.hill-frequency", 0.006),
                    getBiomeFrequency(), getBiomePlainsWeight(), getBiomeDarkForestWeight());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Configuração de terreno inválida; usando o preset clássico: " + e.getMessage());
            return TerrainProfile.classic();
        }
    }

    /** Acesso direto ao FileConfiguration para usos avançados (ex: feast-loot list). */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
