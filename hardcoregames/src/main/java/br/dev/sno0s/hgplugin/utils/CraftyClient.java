package br.dev.sno0s.hgplugin.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.net.ssl.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/** Small HTTP client with bounded requests and safe diagnostics, independent of Bukkit. */
public final class CraftyClient {
    public record Result(boolean success, int status, String message) {}
    private final URI serverEndpoint;
    private final String token;
    private final int timeoutMillis;

    public CraftyClient(String baseUrl, String apiKey, String serverId, int timeoutMillis) {
        String base = baseUrl.strip().replaceAll("/+$", "");
        if (base.endsWith("/api/v2")) base = base.substring(0, base.length() - 7);
        URI origin = URI.create(base);
        if (!("https".equalsIgnoreCase(origin.getScheme()) || "http".equalsIgnoreCase(origin.getScheme()))
                || origin.getHost() == null || origin.getUserInfo() != null
                || origin.getQuery() != null || origin.getFragment() != null) {
            throw new IllegalArgumentException("crafty.url deve ser uma URL HTTP/HTTPS válida, sem credenciais.");
        }
        String id = serverId.strip();
        if (!id.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("crafty.server-id ausente ou inválido.");
        token = apiKey.strip().replaceFirst("(?i)^Bearer\\s+", "");
        if (token.isBlank() || token.contains("\r") || token.contains("\n")) {
            throw new IllegalArgumentException("crafty.api-key ausente ou inválida.");
        }
        serverEndpoint = URI.create(base + "/api/v2/servers/" + id);
        this.timeoutMillis = Math.clamp(timeoutMillis, 100, 60000);
    }

    public Result check() { return request("GET", serverEndpoint); }
    public Result restart() { return request("POST", URI.create(serverEndpoint + "/action/restart_server")); }

    private Result request(String method, URI endpoint) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) endpoint.toURL().openConnection();
            if (connection instanceof HttpsURLConnection https) {
                // Preserve the existing support for Crafty's local self-signed certificate.
                https.setSSLSocketFactory(localCertificateFactory());
                https.setHostnameVerifier((hostname, session) -> true);
            }
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Accept", "application/json");
            if (method.equals("POST")) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(0);
                connection.getOutputStream().close();
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String body = "";
            if (stream != null) {
                try (stream) { body = new String(stream.readNBytes(8192), StandardCharsets.UTF_8); }
            }
            JsonObject json = null;
            try { json = JsonParser.parseString(body).getAsJsonObject(); }
            catch (RuntimeException ignored) { /* A proxy can reply with HTML, even with HTTP 200. */ }
            boolean ok = status >= 200 && status < 300 && json != null && json.has("status")
                    && "ok".equalsIgnoreCase(json.get("status").getAsString());
            if (ok) return new Result(true, status, "Crafty aceitou a requisição (HTTP " + status + ").");
            String hint = switch (status) {
                case 401 -> "token inválido, expirado ou revogado; confira a chave de API no Crafty";
                case 403 -> "sem permissão para este servidor; confira a permissão de comandos da chave/usuário";
                case 404 -> "servidor ou endpoint não encontrado; confira url e server-id";
                case 301, 302, 303, 307, 308 -> "redirecionamento; configure a URL final do Crafty";
                default -> "resposta não aceita; confira o painel/proxy e os logs do Crafty";
            };
            // Error codes are useful; do not echo arbitrary response bodies or credentials.
            String code = "";
            if (json != null && json.has("error") && json.get("error").isJsonPrimitive()) {
                String value = json.get("error").getAsString();
                if (!value.equals(token) && value.matches("[A-Z][A-Z0-9_]{0,79}")) code = " [" + value + "]";
            }
            return new Result(false, status, "HTTP " + status + code + ": " + hint + ".");
        } catch (java.net.SocketTimeoutException e) {
            return new Result(false, 0, "Timeout ao conectar/ler o Crafty; confira endereço, porta, protocolo e rede do servidor.");
        } catch (javax.net.ssl.SSLException e) {
            return new Result(false, 0, "Falha TLS/HTTPS; confira protocolo, porta e proxy do Crafty.");
        } catch (Exception e) {
            return new Result(false, 0, "Falha de comunicação com o Crafty (" + e.getClass().getSimpleName() + "). Confira a rede e a configuração.");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static SSLSocketFactory localCertificateFactory() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
        }}, new SecureRandom());
        return context.getSocketFactory();
    }
}
