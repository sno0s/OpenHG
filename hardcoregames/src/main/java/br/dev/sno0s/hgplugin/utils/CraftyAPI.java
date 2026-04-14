package br.dev.sno0s.hgplugin.utils;

import br.dev.sno0s.hgplugin.ConfigManager;
import br.dev.sno0s.hgplugin.Hgplugin;
import org.bukkit.Bukkit;

import javax.net.ssl.*;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class CraftyAPI {

    private CraftyAPI() {}

    public static void scheduleRestart() {
        ConfigManager cfg = Hgplugin.getConfigManager();
        int delay = cfg.getCraftyRestartDelay();

        new org.bukkit.scheduler.BukkitRunnable() {
            int countdown = delay;

            @Override
            public void run() {
                if (countdown <= 0) {
                    Messages.broadcast("Reiniciando o servidor...");
                    restartAsync();
                    cancel();
                    return;
                }
                if (countdown == delay || countdown <= 5) {
                    Messages.broadcast("Servidor reiniciando em " + Messages.hl(countdown + "s") + "!");
                }
                countdown--;
            }
        }.runTaskTimer(Hgplugin.getInstance(), 0L, 20L);
    }

    private static void restartAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(Hgplugin.getInstance(), () -> {
            ConfigManager cfg = Hgplugin.getConfigManager();
            String baseUrl  = cfg.getCraftyUrl();
            String apiKey   = cfg.getCraftyApiKey();
            String serverId = cfg.getCraftyServerId();

            if (apiKey.isBlank() || serverId.isBlank()) {
                Bukkit.getLogger().warning("[HardcoreGames] Crafty: api-key ou server-id não configurados.");
                return;
            }

            try {
                String endpoint = baseUrl + "/api/v2/servers/" + serverId + "/action/restart_server";

                URL url = new URL(endpoint);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

                // aceita qualquer certificado autoassinado
                conn.setSSLSocketFactory(buildSSLSocketFactory());
                // aceita qualquer hostname (rede local com IP)
                conn.setHostnameVerifier((hostname, session) -> true);

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Length", "0");
                conn.setDoOutput(false);
                conn.connect();

                int status = conn.getResponseCode();
                if (status == 200) {
                    Bukkit.getLogger().info("[HardcoreGames] Crafty: reinício enviado com sucesso.");
                } else {
                    Bukkit.getLogger().warning("[HardcoreGames] Crafty: resposta inesperada " + status);
                }

                conn.disconnect();

            } catch (Exception e) {
                Bukkit.getLogger().severe("[HardcoreGames] Crafty: falha ao enviar reinício — " + e.getMessage());
            }
        });
    }

    private static SSLSocketFactory buildSSLSocketFactory() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
        }, new SecureRandom());
        return ctx.getSocketFactory();
    }
}
