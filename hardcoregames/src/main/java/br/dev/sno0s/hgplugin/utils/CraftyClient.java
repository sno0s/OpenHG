package br.dev.sno0s.hgplugin.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.net.ssl.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/** Cliente da API v2 do Crafty, com timeout e diagnóstico sem expor credenciais. */
public final class CraftyClient {
    public record Result(boolean success, int status, String message) {}
    private final URI serverEndpoint;
    private final String token;
    private final int timeoutMillis;

    public CraftyClient(String baseUrl, String apiKey, String serverId, int timeoutMillis) {
        String base = baseUrl.strip().replaceAll("/+$", "");
        if (base.endsWith("/api/v2")) {
            base = base.substring(0, base.length() - 7);
        }
        URI origin = URI.create(base);
        if (!("https".equalsIgnoreCase(origin.getScheme()) || "http".equalsIgnoreCase(origin.getScheme()))
                || origin.getHost() == null || origin.getUserInfo() != null
                || origin.getQuery() != null || origin.getFragment() != null) {
            throw new IllegalArgumentException(Messages.text("console.crafty-validation.invalid-url"));
        }
        String id = serverId.strip();
        if (!id.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(Messages.text("console.crafty-validation.invalid-server-id"));
        }
        token = apiKey.strip().replaceFirst("(?i)^Bearer\\s+", "");
        if (token.isBlank() || token.contains("\r") || token.contains("\n")) {
            throw new IllegalArgumentException(Messages.text("console.crafty-validation.invalid-token"));
        }
        serverEndpoint = URI.create(base + "/api/v2/servers/" + id);
        this.timeoutMillis = Math.clamp(timeoutMillis, 100, 60000);
    }

    public Result check() {
        return request("GET", serverEndpoint);
    }

    public Result restart() {
        return request("POST", URI.create(serverEndpoint + "/action/restart_server"));
    }

    public String getAddress() {
        int port = serverEndpoint.getPort();
        if (port == -1) {
            port = serverEndpoint.getScheme().equalsIgnoreCase("https") ? 443 : 80;
        }
        return serverEndpoint.getScheme() + "://" + serverEndpoint.getHost() + ":" + port;
    }

    private Result request(String method, URI endpoint) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) endpoint.toURL().openConnection();
            if (connection instanceof HttpsURLConnection https) {
                // Mantém o suporte ao certificado autoassinado do painel local.
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
            catch (RuntimeException ignored) { /* Um proxy pode responder HTML mesmo com HTTP 200. */ }
            boolean ok = status >= 200 && status < 300 && json != null && json.has("status")
                    && "ok".equalsIgnoreCase(json.get("status").getAsString());
            if (ok) return new Result(true, status, Messages.text("crafty.accepted", "status", status));
            String hint = switch (status) {
                case 401 -> Messages.text("crafty.http.unauthorized");
                case 403 -> Messages.text("crafty.http.forbidden");
                case 404 -> Messages.text("crafty.http.not-found");
                case 301, 302, 303, 307, 308 -> Messages.text("crafty.http.redirect");
                default -> Messages.text("crafty.http.unexpected-response");
            };
            // Exibe apenas códigos conhecidos, sem copiar respostas arbitrárias ou credenciais.
            String code = "";
            if (json != null && json.has("error") && json.get("error").isJsonPrimitive()) {
                String value = json.get("error").getAsString();
                if (!value.equals(token) && value.matches("[A-Z][A-Z0-9_]{0,79}")) code = " [" + value + "]";
            }
            return new Result(false, status, Messages.text("crafty.http-error", "status", status, "code", code, "hint", hint));
        } catch (ConnectException e) {
            return new Result(false, 0, Messages.text("crafty.connection-refused", "address", getAddress()));
        } catch (NoRouteToHostException e) {
            return new Result(false, 0, Messages.text("crafty.no-route", "address", getAddress()));
        } catch (UnknownHostException e) {
            return new Result(false, 0, Messages.text("crafty.unknown-host", "address", getAddress()));
        } catch (java.net.SocketTimeoutException e) {
            return new Result(false, 0, Messages.text("crafty.timeout", "address", getAddress()));
        } catch (javax.net.ssl.SSLException e) {
            return new Result(false, 0, Messages.text("crafty.tls-error"));
        } catch (Exception e) {
            return new Result(false, 0, Messages.text("crafty.communication-error", "exception", e.getClass().getSimpleName()));
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
