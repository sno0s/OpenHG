package br.dev.sno0s.hgplugin;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameState {

    private static GameState instance;

    public static void init() {
        instance = new GameState();
    }

    public static GameState getInstance() {
        return instance;
    }

    // -------------------------
    // Fase da partida
    // -------------------------
    private MatchPhase phase = MatchPhase.WAITING;

    public MatchPhase getPhase() { return phase; }
    public void setPhase(MatchPhase phase) { this.phase = phase; }

    // -------------------------
    // Jogadores
    // -------------------------
    private final Map<UUID, PlayerData> players = new HashMap<>();

    /** Registra o jogador se ainda não estiver registrado. */
    public void registerPlayer(Player player) {
        players.putIfAbsent(player.getUniqueId(), new PlayerData(player.getName()));
    }

    public PlayerData getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public Map<UUID, PlayerData> getPlayers() {
        return Collections.unmodifiableMap(players);
    }

    // -------------------------
    // Dados por jogador
    // -------------------------
    public static class PlayerData {
        private final String name;
        private String selectedKit = null;
        private int kills = 0;
        private boolean alive = true;

        public PlayerData(String name) {
            this.name = name;
        }

        public String getName() { return name; }

        public String getSelectedKit() { return selectedKit; }
        public void setSelectedKit(String kit) { this.selectedKit = kit; }

        public int getKills() { return kills; }
        public void addKill() { kills++; }

        public boolean isAlive() { return alive; }
        public void setAlive(boolean alive) { this.alive = alive; }
    }
}
