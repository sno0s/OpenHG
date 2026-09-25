package br.dev.sno0s.hgplugin;

import br.dev.sno0s.hgplugin.utils.Messages;
import br.dev.sno0s.hgplugin.listeners.CompassListener;
import br.dev.sno0s.hgplugin.listeners.DeathListener;
import br.dev.sno0s.hgplugin.listeners.DisconnectListener;
import br.dev.sno0s.hgplugin.listeners.KitSelectorListener;
import br.dev.sno0s.hgplugin.listeners.MatchCountDown;
import br.dev.sno0s.hgplugin.listeners.OnDrop;
import br.dev.sno0s.hgplugin.listeners.PlayerJoinListener;
import br.dev.sno0s.hgplugin.listeners.RocketListener;
import br.dev.sno0s.hgplugin.listeners.SoupListener;
import br.dev.sno0s.hgplugin.listeners.StatsListener;
import br.dev.sno0s.hgplugin.listeners.AxeDamageListener;
import br.dev.sno0s.hgplugin.listeners.EnchantListener;
import br.dev.sno0s.hgplugin.listeners.TrashBreakListener;
import br.dev.sno0s.hgplugin.database.Database;
import br.dev.sno0s.hgplugin.database.PlayerStatsDAO;
import br.dev.sno0s.hgplugin.listeners.ChunkDecorationListener;
import br.dev.sno0s.hgplugin.listeners.ArenaStructureListener;
import br.dev.sno0s.hgplugin.utils.SoupRecipes;
import br.dev.sno0s.hgplugin.worldgeneration.WorldGeneration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Hgplugin extends JavaPlugin {

    private static Hgplugin instance;
    private ConfigManager configManager;
    private Database database;
    private PlayerStatsDAO statsDAO;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        Messages.load(this);
        Bukkit.getLogger().info(Messages.log("console.plugin.enabled"));
        configManager = new ConfigManager(this);
        var swordBlocking = new br.dev.sno0s.hgplugin.listeners.SwordBlockingListener(this,
                new br.dev.sno0s.hgplugin.items.SwordBlocking(configManager.isSwordBlockingEnabled(),
                        configManager.getSwordBlockingReduction()));
        getServer().getPluginManager().registerEvents(swordBlocking, this);
        getServer().getOnlinePlayers().forEach(swordBlocking::updateInventory);
        GameState.init();
        for (String name : java.util.List.of("startmatch", "spawnfeast", "kit", "stats", "restarthg")) {
            var command = getCommand(name);
            command.setDescription(Messages.text("commands." + name + ".description"));
            command.setUsage(Messages.text("commands." + name + ".usage"));
            command.setPermissionMessage(Messages.text("prefix") + Messages.text("common.no-permission"));
        }

        database = new Database(this);
        try {
            database.connect();
            statsDAO = new PlayerStatsDAO(database);
        } catch (Exception e) {
            getLogger().severe(Messages.log("console.plugin.database-failed", "detail", e.getMessage()));
        }

        ChunkDecorationListener decorations = new ChunkDecorationListener(this,
                configManager.getMushroomDensity(), configManager.getCocoaDensity());
        getServer().getPluginManager().registerEvents(decorations, this);
        getServer().getPluginManager().registerEvents(new ArenaStructureListener(), this);

        WorldGeneration.execute(this, decorations);
        br.dev.sno0s.hgplugin.listeners.WallContactListener.start(this);
        SoupRecipes.register(this);
        getServer().getPluginManager().registerEvents(new br.dev.sno0s.hgplugin.listeners.MenuDragListener(), this);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new SoupListener(), this);
        getServer().getPluginManager().registerEvents(new OnDrop(), this);
        getServer().getPluginManager().registerEvents(new MatchCountDown(), this);
        getServer().getPluginManager().registerEvents(new CompassListener(), this);
        getServer().getPluginManager().registerEvents(new DeathListener(), this);
        getServer().getPluginManager().registerEvents(new RocketListener(), this);
        getServer().getPluginManager().registerEvents(new KitSelectorListener(), this);
        getServer().getPluginManager().registerEvents(new DisconnectListener(), this);
        getServer().getPluginManager().registerEvents(new StatsListener(), this);
        getServer().getPluginManager().registerEvents(new TrashBreakListener(), this);
        getServer().getPluginManager().registerEvents(new EnchantListener(), this);
        getServer().getPluginManager().registerEvents(new AxeDamageListener(), this);
        getServer().getPluginManager().registerEvents(new br.dev.sno0s.hgplugin.listeners.LumberjackListener(), this);
        getServer().getPluginManager().registerEvents(new br.dev.sno0s.hgplugin.listeners.FishermanListener(), this);

        getCommand("kit").setExecutor(new br.dev.sno0s.hgplugin.commands.KitCommand());
        getCommand("stats").setExecutor(new br.dev.sno0s.hgplugin.commands.StatsCommand());

        getCommand("startmatch").setExecutor(new br.dev.sno0s.hgplugin.commands.StartMatchCommand());
        getCommand("spawnfeast").setExecutor(new br.dev.sno0s.hgplugin.commands.SpawnFeastCommand());
        getCommand("restarthg").setExecutor(new br.dev.sno0s.hgplugin.commands.RestartCommand());
    }

    @Override
    public void onDisable() {
        if (database != null) database.disconnect();
        getLogger().info(Messages.log("console.plugin.disabled"));
    }

    public static Hgplugin getInstance() {
        return instance;
    }

    public static ConfigManager getConfigManager() {
        return instance.configManager;
    }

    public static PlayerStatsDAO getStatsDAO() {
        return instance.statsDAO;
    }
}
