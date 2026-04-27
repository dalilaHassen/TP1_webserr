package server.db;

import common.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO JDBC pour la table `produits`.
 */
public class ProduitDAO {

    private final DatabaseManager dbManager;

    public ProduitDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // ── INSERT ────────────────────────────────────────────────
    public void inserer(Produit p) throws SQLException {
        String sql = "INSERT INTO produits (id, nom, categorie, prix_unitaire) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getNom());
            ps.setString(3, p.getCategorie());
            ps.setDouble(4, p.getPrixUnitaire());
            ps.executeUpdate();
        }
    }

    // ── SELECT ALL ────────────────────────────────────────────
    public List<Produit> listerTous() throws SQLException {
        String sql = "SELECT id, nom, categorie, prix_unitaire FROM produits ORDER BY id";
        List<Produit> liste = new ArrayList<>();
        try (Statement st = dbManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapper(rs));
        }
        return liste;
    }

    // ── SELECT BY ID ──────────────────────────────────────────
    public Produit trouverParId(String id) throws SQLException {
        String sql = "SELECT id, nom, categorie, prix_unitaire FROM produits WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapper(rs) : null;
            }
        }
    }

    // ── EXISTS ────────────────────────────────────────────────
    public boolean existe(String id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM produits WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ── MAPPER ────────────────────────────────────────────────
    private Produit mapper(ResultSet rs) throws SQLException {
        return new Produit(
                rs.getString("id"),
                rs.getString("nom"),
                rs.getString("categorie"),
                rs.getDouble("prix_unitaire")
        );
    }
}