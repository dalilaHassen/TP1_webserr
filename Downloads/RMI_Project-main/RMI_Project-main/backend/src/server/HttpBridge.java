package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HttpBridge {

    private AuthentificationImpl auth;

    public HttpBridge(AuthentificationImpl auth) {
        this.auth = auth;
    }

    public void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // ─── CORS pour toutes les routes ───
        server.createContext("/api/auth/login",  this::handleLogin);
        server.createContext("/api/auth/logout", this::handleLogout);

        server.setExecutor(null);
        server.start();
        System.out.println("✅ HTTP Bridge démarré sur port 8080");
    }

    // ─────────────────────────────────────────
    // POST /api/auth/login
    // ─────────────────────────────────────────
    private void handleLogin(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        // Répondre au preflight OPTIONS (demande du navigateur)
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            // Lire le body JSON
            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            // Parser manuellement (pas besoin de librairie)
            String username = extractJson(body, "username");
            String password = extractJson(body, "password");

            System.out.println("🔐 Tentative login: " + username);

            boolean ok = auth.seConnecter(username, password);

            String json;
            int status;

            if (ok) {
                json = String.format(
                        "{\"success\":true,\"message\":\"Connexion réussie\",\"username\":\"%s\",\"user\":{\"username\":\"%s\",\"role\":\"CHEF_ENTREPOT\"}}",
                        username, username
                );
                status = 200;
                System.out.println("✅ Login réussi: " + username);
            } else {
                json = "{\"success\":false,\"message\":\"Email ou mot de passe incorrect\"}";
                status = 401;
                System.out.println("❌ Login échoué: " + username);
            }

            sendJson(exchange, status, json);

        } catch (Exception e) {
            String json = "{\"success\":false,\"message\":\"Erreur serveur: " + e.getMessage() + "\"}";
            sendJson(exchange, 500, json);
            System.err.println("❌ Erreur: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // POST /api/auth/logout?username=xxx
    // ─────────────────────────────────────────
    private void handleLogout(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String query = exchange.getRequestURI().getQuery(); // username=xxx
            String username = query != null ? query.replace("username=", "") : "";

            auth.seDeconnecter(username);

            sendJson(exchange, 200, "{\"success\":true,\"message\":\"Déconnecté\"}");
            System.out.println("👋 Logout: " + username);

        } catch (Exception e) {
            sendJson(exchange, 500, "{\"success\":false,\"message\":\"Erreur logout\"}");
        }
    }

    // ─────────────────────────────────────────
    // Utilitaires
    // ─────────────────────────────────────────

    // Envoyer une réponse JSON
    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Ajouter les headers CORS (nécessaire pour que Vue puisse appeler)
    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    // Parser JSON simple sans librairie
    // Ex: {"username":"test@mail.com","password":"1234"} → extrait "test@mail.com"
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }
}