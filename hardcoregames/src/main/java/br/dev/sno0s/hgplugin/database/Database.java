package br.dev.sno0s.hgplugin.database;

import br.dev.sno0s.hgplugin.utils.Messages;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private Connection connection;
    private final JavaPlugin plugin;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), "hg_stats.db");
        plugin.getDataFolder().mkdirs();

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        createTable();
        plugin.getLogger().info(Messages.log("console.database.connected"));
    }

    private void createTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid      TEXT PRIMARY KEY,
                    name      TEXT    NOT NULL,
                    kills     INTEGER NOT NULL DEFAULT 0,
                    deaths    INTEGER NOT NULL DEFAULT 0,
                    wins      INTEGER NOT NULL DEFAULT 0,
                    matches   INTEGER NOT NULL DEFAULT 0,
                    last_kit  TEXT    NOT NULL DEFAULT 'Nenhum'
                )
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
        // migração para bancos já existentes sem a coluna last_kit
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN last_kit TEXT NOT NULL DEFAULT 'Nenhum'");
        } catch (SQLException ignored) {
            // coluna já existe — ignorar
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info(Messages.log("console.database.disconnected"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning(Messages.log("console.database.close-failed", "detail", e.getMessage()));
        }
    }
}
