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
    // ── INSERT ────────────────────────────────────────────────
    public void inserer(Produit p) throws SQLException {
        String sql = "INSERT INTO produits (id, nom, categorie, description, prix_unitaire) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getNom());
            ps.setString(3, p.getCategorie());
            ps.setString(4, p.getDescription() == null ? "" : p.getDescription()); // ← ajout
            ps.setDouble(5, p.getPrixUnitaire());
            ps.executeUpdate();
        }
    }

    // ── SELECT ALL ────────────────────────────────────────────
    public List<Produit> listerTous() throws SQLException {
        String sql = "SELECT id, nom, categorie, description, prix_unitaire FROM produits ORDER BY id";
        List<Produit> liste = new ArrayList<>();
        try (Statement st = dbManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapper(rs));
        }
        return liste;
    }

    // ── SELECT BY ID ──────────────────────────────────────────
    public Produit trouverParId(String id) throws SQLException {
        String sql = "SELECT id, nom, categorie, description, prix_unitaire FROM produits WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapper(rs) : null;
            }
        }
    }

    // ── UPDATE ────────────────────────────────────────────────
    public void modifier(String id, String nom, String categorie, double prixUnitaire)
            throws SQLException {
        String sql = "UPDATE produits SET nom = ?, categorie = ?, prix_unitaire = ? WHERE id = ?";
        //                                                         ↑ correction: prix → prix_unitaire
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, categorie);
            ps.setDouble(3, prixUnitaire);
            ps.setString(4, id);
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────
    public void supprimer(String id) throws SQLException {
        // D'abord supprimer les mouvements liés
        String sqlMouv = "DELETE FROM mouvements WHERE id_produit = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sqlMouv)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }

        // Ensuite supprimer les stocks liés
        String sqlStock = "DELETE FROM stocks WHERE id_produit = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sqlStock)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }

        // Enfin supprimer le produit
        String sqlProduit = "DELETE FROM produits WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sqlProduit)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }
    // ── MAPPER ────────────────────────────────────────────────
    private Produit mapper(ResultSet rs) throws SQLException {
        return new Produit(
                rs.getString("id"),
                rs.getString("nom"),
                rs.getString("categorie"),
                rs.getString("description"), // ← ajout
                rs.getDouble("prix_unitaire")
        );
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
}