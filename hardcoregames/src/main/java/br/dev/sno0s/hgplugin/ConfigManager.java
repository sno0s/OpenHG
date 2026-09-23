package br.dev.sno0s.hgplugin;

import br.dev.sno0s.hgplugin.utils.Messages;
import br.dev.sno0s.hgplugin.utils.YamlFiles;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import br.dev.sno0s.hgplugin.worldgeneration.TerrainProfile;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    static final String LEGACY_LOOT = "HGconfigs.feast-loot";

    private final Hgplugin plugin;
    private final File feastFile;
    private final YamlConfiguration feast;

    public ConfigManager(Hgplugin plugin) {
        this.plugin = plugin;
        // Test doubles podem não expor a pasta de dados; o servidor sempre expõe.
        File folder = plugin.getDataFolder();
        // JavaPlugin#getConfig pode mascarar YAML inválido usando defaults; valide o disco primeiro.
        if (folder != null) YamlFiles.read(new File(folder, "config.yml"));
        this.feastFile = folder == null ? null : new File(folder, "feast.yml");
        this.feast = YamlFiles.read(feastFile);

        boolean changed = mergeBundledDefaults(plugin.getConfig());
        if (migrateTerrainDefaults(plugin.getConfig())) {
            changed = true;
        }
        if (adoptFeastLoot()) {
            // O loot sai do config.yml; a cópia preserva a lista original do servidor.
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
        return YamlFiles.mergeDefaults(config, bundled);
    }

    /**
     * Move o loot do config.yml para feast.yml uma única vez, preservando a lista
     * do servidor. Instalações novas recebem a lista embutida no JAR.
     * Retorna true quando o config.yml precisa ser regravado sem o loot antigo.
     */
    boolean adoptFeastLoot() {
        if (feastFile == null) return false;
        FileConfiguration config = plugin.getConfig();
        boolean legacy = config.isSet(LEGACY_LOOT);
        boolean changed = false;
        if (legacy) {
            YamlFiles.backup(new File(plugin.getDataFolder(), "config.yml"));
            YamlFiles.backup(feastFile);
            // Um loot já definido no feast.yml é a escolha mais recente e prevalece.
            if (!feast.isSet("loot")) {
                feast.set("loot", config.get(LEGACY_LOOT));
                changed = true;
            } else if (!java.util.Objects.equals(feast.get("loot"), config.get(LEGACY_LOOT))) {
                plugin.getLogger().warning(Messages.log("console.messages.conflicting-key", "key", LEGACY_LOOT));
            }
        }
        if (!feast.isSet("loot")) {
            Object bundled = YamlFiles.bundled("feast.yml").get("loot");
            if (bundled != null) {
                feast.set("loot", bundled);
                changed = true;
            }
        }
        if (changed) {
            feast.options().header(YamlFiles.bundled("feast.yml").options().header());
            YamlFiles.save(feast, feastFile);
        }
        // Só remove a origem depois de salvar o destino com sucesso.
        if (legacy) config.set(LEGACY_LOOT, null);
        return legacy;
    }

    /**
     * Entradas de loot do feast.yml. Uma lista malformada é reportada no console
     * e nunca substituída em silêncio pelos padrões.
     */
    public List<Map<?, ?>> getFeastLoot() {
        Object raw = feast.get("loot");
        if (raw == null) return List.of();
        if (!(raw instanceof List<?> entries)) {
            plugin.getLogger().warning(Messages.log("console.feast.invalid-loot"));
            return List.of();
        }
        List<Map<?, ?>> loot = new ArrayList<>();
        for (Object entry : entries) {
            if (entry instanceof Map<?, ?> item) loot.add(item);
            else plugin.getLogger().warning(Messages.log("console.feast.invalid-entry", "entry", entry));
        }
        return loot;
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

    public boolean isSwordBlockingEnabled() {
        return plugin.getConfig().getBoolean("HGconfigs.combat.sword-blocking.enabled", true);
    }

    public float getSwordBlockingReduction() {
        double value = plugin.getConfig().getDouble("HGconfigs.combat.sword-blocking.damage-reduction", 1.0);
        if (Double.isFinite(value) && value >= 0 && value <= 20) return (float) value;
        plugin.getLogger().warning(Messages.log("console.config-manager.invalid-sword-blocking"));
        return 1.0f;
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

    /** Acesso direto ao FileConfiguration para usos avançados. */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
