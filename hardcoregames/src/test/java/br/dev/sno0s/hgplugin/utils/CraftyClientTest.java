package br.dev.sno0s.hgplugin.utils;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class CraftyClientTest {
    private HttpServer server;
    private String url;
    private int status = 200;
    private String body = "{\"status\":\"ok\"}";
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
            exchange.close();
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach void stop() { server.stop(0); }

    @Test
    void restartUsesDocumentedEndpointEmptyPostAndNormalizedBearer() {
        CraftyClient.Result result = new CraftyClient(url + "/api/v2/", " Bearer test-token ", " server-123 ", 1000).restart();
        assertTrue(result.success(), result.message());
        assertEquals("POST", method.get());
        assertEquals("/api/v2/servers/server-123/action/restart_server", path.get());
        assertEquals("Bearer test-token", auth.get());
    }

    @Test
    void checkIsReadOnlyAndRequiresAnApiSuccessResponse() {
        CraftyClient client = new CraftyClient(url, "test-token", "server-123", 1000);
        assertTrue(client.check().success());
        assertEquals("GET", method.get());
        assertEquals("/api/v2/servers/server-123", path.get());
        body = "<html>Login</html>";
        assertFalse(client.check().success());
        body = "{\"status\":\"error\",\"error\":\"NOT_AUTHORIZED\"}";
        assertFalse(client.check().success());
    }

    @Test
    void errorsDistinguishTokenPermissionsAndMissingServerWithoutExposingSecrets() {
        CraftyClient client = new CraftyClient(url, "test-token", "server-123", 1000);
        body = "{\"status\":\"error\",\"error\":\"test-token\"}";
        status = 401;
        assertTrue(client.restart().message().contains("token inválido"));
        assertFalse(client.restart().message().contains("test-token"));
        status = 403;
        assertTrue(client.restart().message().contains("sem permissão"));
        status = 404;
        assertTrue(client.restart().message().contains("não encontrado"));
        status = 302;
        assertFalse(client.restart().success());
        status = 500;
        assertFalse(client.restart().success());
    }

    @Test
    void refusedConnectionIdentifiesAddressAndFailureBeforeTheApi() throws Exception {
        int port;
        try (var socket = new java.net.ServerSocket(0, 0, java.net.InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        String address = "http://127.0.0.1:" + port;
        CraftyClient.Result result = new CraftyClient(address, "test-token", "server-123", 500).check();
        assertFalse(result.success());
        assertEquals(0, result.status());
        assertTrue(result.message().contains("ConnectException"));
        assertTrue(result.message().contains(address));
        assertTrue(result.message().contains("A API não respondeu"));
        assertFalse(result.message().contains("test-token"));
    }

    @Test
    void invalidConfigurationFailsBeforeSendingAnything() {
        assertThrows(IllegalArgumentException.class, () -> new CraftyClient("file:///tmp", "token", "id", 1000));
        assertThrows(IllegalArgumentException.class, () -> new CraftyClient(url, "", "id", 1000));
        assertThrows(IllegalArgumentException.class, () -> new CraftyClient(url, "token", "../id", 1000));
        assertNull(method.get());
    }
}
