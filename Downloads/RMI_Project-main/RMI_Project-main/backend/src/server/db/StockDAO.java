package server.db;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO JDBC pour la table `stocks`.
 * Toutes les opérations de modification passent par des transactions
 * explicites gérées dans GestionStocksImpl.
 */
public class StockDAO {

    private final DatabaseManager dbManager;

    public StockDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // ── Lire la quantité courante ─────────────────────────────
    public int getQuantite(String idEntrepot, String idProduit) throws SQLException {
        String sql = "SELECT quantite FROM stocks WHERE id_entrepot = ? AND id_produit = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, idEntrepot);
            ps.setString(2, idProduit);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("quantite") : 0;
            }
        }
    }

    /**
     * Ajoute `delta` à la quantité (positif = entrée, négatif = sortie).
     * Crée la ligne si elle n'existe pas encore (INSERT … ON DUPLICATE KEY UPDATE).
     * Lève une SQLException si la quantité résultante serait négative
     * (contrainte CHECK dans le schéma + contrôle applicatif).
     */
    public void ajusterQuantite(Connection conn,
                                String idEntrepot, String idProduit,
                                int delta) throws SQLException {
        String sql = """
                INSERT INTO stocks (id_entrepot, id_produit, quantite)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE quantite = quantite + ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // INSERT initial avec delta (si ligne inexistante)
            ps.setString(1, idEntrepot);
            ps.setString(2, idProduit);
            ps.setInt   (3, Math.max(delta, 0)); // entrée initiale
            ps.setInt   (4, delta);              // ajustement si ligne existante
            ps.executeUpdate();
        }
    }

    // ── Vue complète d'un entrepôt → Map idProduit → quantité ─
    public Map<String, Integer> getStocksEntrepot(String idEntrepot) throws SQLException {
        String sql = "SELECT id_produit, quantite FROM stocks WHERE id_entrepot = ?";
        Map<String, Integer> map = new HashMap<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, idEntrepot);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    map.put(rs.getString("id_produit"), rs.getInt("quantite"));
            }
        }
        return map;
    }

    // ── Répartition d'un produit dans tous les entrepôts ──────
    public Map<String, Integer> getStocksProduitTousEntrepots(String idProduit)
            throws SQLException {
        String sql = "SELECT id_entrepot, quantite FROM stocks WHERE id_produit = ?";
        Map<String, Integer> map = new HashMap<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, idProduit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    map.put(rs.getString("id_entrepot"), rs.getInt("quantite"));
            }
        }
        return map;
    }

    // ── Produits dont le stock total < seuil ──────────────────
    public java.util.List<String> getProduitsSousSeuilGlobal(int seuil)
            throws SQLException {
        String sql = """
                SELECT id_produit
                FROM stocks
                GROUP BY id_produit
                HAVING SUM(quantite) < ?
                """;
        java.util.List<String> liste = new java.util.ArrayList<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, seuil);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(rs.getString("id_produit"));
            }
        }
        return liste;
    }
}