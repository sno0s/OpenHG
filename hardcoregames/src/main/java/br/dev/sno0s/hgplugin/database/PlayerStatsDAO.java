package br.dev.sno0s.hgplugin.database;

import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerStatsDAO {

    private final Database database;

    public PlayerStatsDAO(Database database) {
        this.database = database;
    }

    // -------------------------
    // Garante que o jogador existe na tabela
    // -------------------------

    public void ensureExists(UUID uuid, String name) {
        String sql = "INSERT OR IGNORE INTO player_stats (uuid, name) VALUES (?, ?)";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            log("Erro ao criar jogador: " + e.getMessage());
        }
    }

    // -------------------------
    // Incrementos (chamados nos eventos)
    // -------------------------

    public void addKill(UUID uuid) {
        increment(uuid, "kills", 1);
    }

    public void addDeath(UUID uuid) {
        increment(uuid, "deaths", 1);
    }

    public void addWin(UUID uuid) {
        increment(uuid, "wins", 1);
    }

    public void addMatch(UUID uuid) {
        increment(uuid, "matches", 1);
    }

    public void updateLastKit(UUID uuid, String kitName) {
        String sql = "UPDATE player_stats SET last_kit = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, kitName);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log("Erro ao atualizar last_kit: " + e.getMessage());
        }
    }

    private void increment(UUID uuid, String column, int amount) {
        String sql = "UPDATE player_stats SET " + column + " = " + column + " + ? WHERE uuid = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log("Erro ao incrementar " + column + ": " + e.getMessage());
        }
    }

    // -------------------------
    // Leitura
    // -------------------------

    public PlayerStats load(UUID uuid) {
        String sql = "SELECT * FROM player_stats WHERE uuid = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return fromResultSet(rs);
        } catch (SQLException e) {
            log("Erro ao carregar stats: " + e.getMessage());
        }
        return null;
    }

    public PlayerStats loadByName(String name) {
        String sql = "SELECT * FROM player_stats WHERE name = ? COLLATE NOCASE LIMIT 1";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return fromResultSet(rs);
        } catch (SQLException e) {
            log("Erro ao buscar jogador por nome: " + e.getMessage());
        }
        return null;
    }

    /** Top N jogadores por kills. */
    public List<PlayerStats> getTopKills(int limit) {
        return getTop("kills", limit);
    }

    /** Top N jogadores por vitórias. */
    public List<PlayerStats> getTopWins(int limit) {
        return getTop("wins", limit);
    }

    private List<PlayerStats> getTop(String column, int limit) {
        List<PlayerStats> list = new ArrayList<>();
        String sql = "SELECT * FROM player_stats ORDER BY " + column + " DESC LIMIT ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(fromResultSet(rs));
        } catch (SQLException e) {
            log("Erro ao buscar ranking: " + e.getMessage());
        }
        return list;
    }

    // -------------------------
    // Utilitários
    // -------------------------

    private PlayerStats fromResultSet(ResultSet rs) throws SQLException {
        return new PlayerStats(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("name"),
                rs.getInt("kills"),
                rs.getInt("deaths"),
                rs.getInt("wins"),
                rs.getInt("matches"),
                rs.getString("last_kit")
        );
    }

    private Connection connection() {
        return database.getConnection();
    }

    private void log(String msg) {
        Bukkit.getLogger().warning("[HardcoreGames] " + msg);
    }
}
