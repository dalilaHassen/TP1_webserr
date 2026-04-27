package server.db;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO JDBC pour la table `entrepots`.
 */
public class EntrepotDAO {

    private final DatabaseManager dbManager;

    public EntrepotDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // ── INSERT ────────────────────────────────────────────────
    public void inserer(String id, String nom, String ville) throws SQLException {
        String sql = "INSERT INTO entrepots (id, nom, ville) VALUES (?, ?, ?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, nom);
            ps.setString(3, ville);
            ps.executeUpdate();
        }
    }

    // ── SELECT ALL → Map id → "nom (ville)" ──────────────────
    public Map<String, String> listerTous() throws SQLException {
        String sql = "SELECT id, nom, ville FROM entrepots ORDER BY id";
        Map<String, String> map = new HashMap<>();
        try (Statement st = dbManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                map.put(rs.getString("id"),
                        rs.getString("nom") + " (" + rs.getString("ville") + ")");
        }
        return map;
    }

    // ── EXISTS ────────────────────────────────────────────────
    public boolean existe(String id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM entrepots WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}