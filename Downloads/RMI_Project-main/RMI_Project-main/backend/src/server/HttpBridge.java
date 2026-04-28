package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import common.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpBridge {

    private AuthentificationImpl auth;
    private IGestionStocks stocks;

    public HttpBridge(AuthentificationImpl auth, IGestionStocks stocks) {
        this.auth = auth;
        this.stocks = stocks;
    }

    public void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // AUTH
        server.createContext("/api/auth/login",  this::handleLogin);
        server.createContext("/api/auth/logout", this::handleLogout);

        // DATA
        server.createContext("/api/produits",  this::handleProduits);
        server.createContext("/api/entrepots", this::handleEntrepots);

        // STOCKS
        server.createContext("/api/stocks/entrepot",  this::handleStockEntrepot);
        server.createContext("/api/stocks/entree",    this::handleEntree);
        server.createContext("/api/stocks/sortie",    this::handleSortie);
        server.createContext("/api/stocks/transfert", this::handleTransfert);
        server.createContext("/api/stocks/historique", this::handleHistorique);
        server.createContext("/api/stocks/alertes",   this::handleAlertes);

        server.setExecutor(null);
        server.start();

        System.out.println("✅ HTTP Bridge démarré sur http://localhost:8080");
    }

    // ───────────────────────── LOGIN ─────────────────────────
    private void handleLogin(HttpExchange ex) throws IOException {
        addCors(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        String body     = readBody(ex);
        String username = extract(body, "username");
        String password = extract(body, "password");

        boolean ok = auth.seConnecter(username, password);
        send(ex, ok ? 200 : 401, ok ? "{\"success\":true}" : "{\"success\":false}");
    }

    private void handleLogout(HttpExchange ex) throws IOException {
        addCors(ex);
        send(ex, 200, "{\"success\":true}");
    }

    // ───────────────────────── PRODUITS ─────────────────────────
    /**
     * GET    /api/produits          → liste tous les produits
     * POST   /api/produits          → ajouter un produit
     * PUT    /api/produits/{id}     → modifier nom/description/prixUnitaire
     * DELETE /api/produits/{id}     → supprimer un produit
     */
    private void handleProduits(HttpExchange ex) throws IOException {
        addCors(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath(); // ex: /api/produits  ou /api/produits/P001

        // Extraire l'id éventuel depuis le path  (/api/produits/{id})
        String idFromPath = null;
        if (path.length() > "/api/produits/".length()) {
            idFromPath = path.substring("/api/produits/".length());
        }

        try {
            switch (method) {

                // ── GET /api/produits ──────────────────────────────────
                case "GET": {
                    List<Produit> list = stocks.listerProduits();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Produit p = list.get(i);
                        if (i > 0) sb.append(",");
                        sb.append(produitToJson(p));
                    }
                    sb.append("]");
                    send(ex, 200, sb.toString());
                    break;
                }

                // ── POST /api/produits ─────────────────────────────────
                case "POST": {
                    String body = readBody(ex);
                    String prixStr = extractOrDefault(body, "prixUnitaire", "0").trim();
                    double prix = 0.0;
                    try { prix = Double.parseDouble(prixStr); } catch (NumberFormatException ignored) {}

                    Produit p = new Produit(
                            extract(body, "id"),
                            extract(body, "nom"),
                            extractOrDefault(body, "categorie", ""),
                            extractOrDefault(body, "description", ""),
                            prix
                    );
                    stocks.ajouterProduit(p);
                    send(ex, 200, "{\"success\":true}");
                    break;
                }

                // ── PUT /api/produits/{id} ─────────────────────────────
                // Met à jour nom, description et prixUnitaire d'un produit existant.
                // Utilise getProduit() pour récupérer l'existant, puis recrée l'objet
                // avec les nouvelles valeurs et appelle ajouterProduit après suppression,
                // ou mieux : implémente une méthode modifierProduit() dans GestionStocksImpl.
                //
                // Solution simple sans modifier l'interface RMI :
                // On lit le produit existant et on remplace les champs fournis.
                case "PUT": {
                    if (idFromPath == null || idFromPath.isEmpty()) {
                        send(ex, 400, "{\"error\":\"ID manquant dans le path\"}");
                        break;
                    }
                    String body = readBody(ex);

                    // Récupérer le produit existant
                    Produit existant = stocks.getProduit(idFromPath);

                    // Fusionner avec les nouvelles valeurs (garder l'existant si champ absent)
                    String nouveauNom  = extractOrDefault(body, "nom",
                            existant.getNom());
                    String nouvelleCat = extractOrDefault(body, "categorie",
                            existant.getCategorie() == null ? "" : existant.getCategorie());
                    double nouveauPrix = Double.parseDouble(
                            extractOrDefault(body, "prixUnitaire",
                                    String.valueOf(existant.getPrixUnitaire())));

                    ((GestionStocksImpl) stocks).modifierProduit(
                            idFromPath, nouveauNom, nouvelleCat, nouveauPrix);

                    send(ex, 200, "{\"success\":true}");
                    break;
                }

                // ── DELETE /api/produits/{id} ──────────────────────────
                case "DELETE": {
                    if (idFromPath == null || idFromPath.isEmpty()) {
                        send(ex, 400, "{\"error\":\"ID manquant dans le path\"}");
                        break;
                    }
                    ((GestionStocksImpl) stocks).supprimerProduit(idFromPath);
                    send(ex, 200, "{\"success\":true}");
                    break;
                }

                default:
                    send(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
            }

        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    /** Sérialise un Produit en JSON. */
    private String produitToJson(Produit p) {
        return String.format(java.util.Locale.US,  // ← forcer le point décimal
                "{\"id\":\"%s\",\"nom\":\"%s\",\"description\":\"%s\"," +
                        "\"categorie\":\"%s\",\"prixUnitaire\":%.3f}",
                escapeJson(p.getId()),
                escapeJson(p.getNom()),
                escapeJson(p.getDescription() == null ? "" : p.getDescription()),
                escapeJson(p.getCategorie()   == null ? "" : p.getCategorie()),
                p.getPrixUnitaire()
        );
    }

    // ───────────────────────── ENTREPOTS ─────────────────────────
    /**
     * GET  /api/entrepots   → liste tous les entrepôts
     * POST /api/entrepots   → ajouter un entrepôt
     */
    private void handleEntrepots(HttpExchange ex) throws IOException {
        addCors(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            switch (ex.getRequestMethod()) {

                case "GET": {
                    Map<String, String> map = stocks.listerEntrepots();
                    StringBuilder sb = new StringBuilder("[");
                    boolean first = true;
                    for (Map.Entry<String, String> e : map.entrySet()) {
                        if (!first) sb.append(",");
                        // Inclure la ville si disponible via EntrepotDAO
                        sb.append(String.format(
                                "{\"id\":\"%s\",\"nom\":\"%s\"}",
                                escapeJson(e.getKey()), escapeJson(e.getValue())
                        ));
                        first = false;
                    }
                    sb.append("]");
                    send(ex, 200, sb.toString());
                    break;
                }

                case "POST": {
                    String body = readBody(ex);
                    stocks.ajouterEntrepot(
                            extract(body, "id"),
                            extract(body, "nom"),
                            extractOrDefault(body, "ville", "")
                    );
                    send(ex, 200, "{\"success\":true}");
                    break;
                }

                default:
                    send(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
            }

        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // ───────────────────────── STOCK PAR ENTREPOT ─────────────────────────
    private void handleStockEntrepot(HttpExchange ex) throws IOException {
        addCors(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String query = ex.getRequestURI().getQuery(); // "id=E01"
            String id    = parseQueryParam(query, "id");

            Map<String, Integer> stocksMap = stocks.getStockEntrepot(id);

            StringBuilder sb = new StringBuilder();
            sb.append("{\"entrepot\":\"").append(escapeJson(id)).append("\",\"stocks\":[");
            boolean first = true;
            for (Map.Entry<String, Integer> e : stocksMap.entrySet()) {
                if (!first) sb.append(",");
                sb.append(String.format(
                        "{\"idProduit\":\"%s\",\"quantite\":%d}",
                        escapeJson(e.getKey()), e.getValue()
                ));
                first = false;
            }
            sb.append("]}");

            send(ex, 200, sb.toString());

        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // ───────────────────────── ENTREE ─────────────────────────
    private void handleEntree(HttpExchange ex) throws IOException {
        addCors(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String body = readBody(ex);
            stocks.entreeStock(
                    extract(body, "idEntrepot"),
                    extract(body, "idProduit"),
                    Integer.parseInt(extract(body, "quantite")),
                    extractOrDefault(body, "note", "")
            );
            send(ex, 200, "{\"success\":true}");
        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // ───────────────────────── SORTIE ─────────────────────────
    private void handleSortie(HttpExchange ex) throws IOException {
        addCors(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String body = readBody(ex);
            stocks.sortieStock(
                    extract(body, "idEntrepot"),
                    extract(body, "idProduit"),
                    Integer.parseInt(extract(body, "quantite")),
                    extractOrDefault(body, "note", "")
            );
            send(ex, 200, "{\"success\":true}");
        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // ───────────────────────── TRANSFERT ─────────────────────────
    private void handleTransfert(HttpExchange ex) throws IOException {
        addCors(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String body = readBody(ex);
            stocks.transfererStock(
                    extract(body, "idSrc"),
                    extract(body, "idDest"),
                    extract(body, "idProduit"),
                    Integer.parseInt(extract(body, "quantite"))
            );
            send(ex, 200, "{\"success\":true}");
        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // ───────────────────────── HISTORIQUE ─────────────────────────
    private void handleHistorique(HttpExchange ex) throws IOException {
        addCors(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String query      = ex.getRequestURI().getQuery();
            String idEntrepot = parseQueryParam(query, "idEntrepot");

            List<MouvementStock> list = stocks.getHistoriqueEntrepot(idEntrepot);

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                MouvementStock m = list.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(
                        "{\"idProduit\":\"%s\",\"idEntrepot\":\"%s\",\"type\":\"%s\"," +
                                "\"quantite\":%d,\"note\":\"%s\",\"date\":\"%s\"}",
                        escapeJson(m.getIdProduit()),
                        escapeJson(m.getIdEntrepot()),
                        m.getType().name(),
                        m.getQuantite(),
                        escapeJson(m.getNote() == null ? "" : m.getNote()),
                        m.getDateHeure() == null ? "" : m.getDateHeure().toString()
                ));
            }
            sb.append("]");

            send(ex, 200, sb.toString());

        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // ───────────────────────── ALERTES ─────────────────────────
    private void handleAlertes(HttpExchange ex) throws IOException {
        addCors(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String query = ex.getRequestURI().getQuery(); // "seuil=10"
            int    seuil = Integer.parseInt(parseQueryParam(query, "seuil"));

            List<String> list = stocks.alertesStockBas(seuil);

            StringBuilder sb = new StringBuilder("{\"alertes\":[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(list.get(i))).append("\"");
            }
            sb.append("]}");

            send(ex, 200, sb.toString());

        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // ═══════════════════ UTILITAIRES PRIVÉS ══════════════════════

    /** Lit le corps de la requête en UTF-8. */
    private String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Envoie une réponse JSON. */
    private void send(HttpExchange ex, int code, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    /** Ajoute les en-têtes CORS + gère la pré-vérification OPTIONS. */
    private void addCors(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    /**
     * Extraction naïve d'une valeur JSON par clé.
     * Fonctionne pour les chaînes et les nombres simples (pas de JSON imbriqué).
     */
    private String extract(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "";

        start += search.length();
        // Ignorer les espaces éventuels
        while (start < json.length() && json.charAt(start) == ' ') start++;

        if (start >= json.length()) return "";

        if (json.charAt(start) == '"') {
            // Valeur chaîne
            start++;
            int end = json.indexOf("\"", start);
            return end == -1 ? "" : json.substring(start, end);
        } else {
            // Valeur numérique ou booléenne
            int end = json.indexOf(",", start);
            int endBrace = json.indexOf("}", start);
            if (end == -1 || (endBrace != -1 && endBrace < end)) end = endBrace;
            return end == -1 ? json.substring(start).trim() : json.substring(start, end).trim();
        }
    }

    /** Comme extract() mais retourne defaultValue si la clé est absente. */
    private String extractOrDefault(String json, String key, String defaultValue) {
        String v = extract(json, key);
        return (v == null || v.isEmpty()) ? defaultValue : v;
    }

    /**
     * Parse un paramètre de query string simple.
     * Ex: parseQueryParam("id=E01&foo=bar", "id") → "E01"
     */
    private String parseQueryParam(String query, String param) {
        if (query == null) return "";
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) return kv[1];
        }
        return "";
    }

    /** Échappe les caractères spéciaux pour JSON (guillemets, backslash, etc.). */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** Sérialise un Produit en JSON. */

}