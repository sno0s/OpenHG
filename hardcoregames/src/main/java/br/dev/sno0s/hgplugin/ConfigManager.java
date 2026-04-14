package br.dev.sno0s.hgplugin;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final Hgplugin plugin;

    public ConfigManager(Hgplugin plugin) {
        this.plugin = plugin;
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
        return plugin.getConfig().getString("HGconfigs.crafty.url", "https://localhost:8443");
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
        return plugin.getConfig().getDouble("HGconfigs.terrain.biome-frequency", 0.003);
    }

    public double getBiomePlainsWeight() {
        return plugin.getConfig().getDouble("HGconfigs.terrain.plains-weight", 0.2);
    }

    public double getBiomeDarkForestWeight() {
        return plugin.getConfig().getDouble("HGconfigs.terrain.dark-forest-weight", -0.3);
    }

    /** Acesso direto ao FileConfiguration para usos avançados (ex: feast-loot list). */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
