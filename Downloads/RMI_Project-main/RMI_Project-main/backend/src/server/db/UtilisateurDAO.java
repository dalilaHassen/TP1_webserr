package server.db;

import common.Utilisateur;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UtilisateurDAO {
    private static final Logger logger = Logger.getLogger(UtilisateurDAO.class.getName());
    private DatabaseManager dbManager;

    public UtilisateurDAO() {
        try {
            this.dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'initialisation de DatabaseManager", e);
            throw new RuntimeException("Impossible d'initialiser la connexion à la base de données", e);
        }
    }

    /**
     * Ajoute un nouvel utilisateur (compatible avec IAuthentification)
     */
    public boolean ajouterUtilisateur(Utilisateur utilisateur) {
        // Utiliser TOUTES les colonnes existantes
        String sql = "INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role, est_connecte) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, utilisateur.getNom());
            pstmt.setString(2, utilisateur.getPrenom() != null ? utilisateur.getPrenom() : "");
            pstmt.setString(3, utilisateur.getEmail());
            pstmt.setString(4, utilisateur.getMotDePasse());
            pstmt.setString(5, utilisateur.getRole() != null ? utilisateur.getRole() : "CHEF_ENTREPOT");
            pstmt.setBoolean(6, false);

            System.out.println("[DEBUG] SQL: " + sql);
            System.out.println("[DEBUG] Nom: " + utilisateur.getNom());
            System.out.println("[DEBUG] Email: " + utilisateur.getEmail());
            System.out.println("[DEBUG] Mot de passe: " + utilisateur.getMotDePasse());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        utilisateur.setId(rs.getInt(1));
                    }
                }
                System.out.println("[DB] ✅ Utilisateur ajouté avec succès! ID: " + utilisateur.getId());
                return true;
            } else {
                System.out.println("[DB] ❌ Aucune ligne insérée");
            }
        } catch (SQLException e) {
            System.err.println("[DB] ❌ Erreur SQL: " + e.getMessage());
            System.err.println("[DB] SQL State: " + e.getSQLState());
            System.err.println("[DB] Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Authentifie un utilisateur par username (email) et mot de passe
     */
    public boolean authentifier(String username, String motDePasse) {
        String sql = "SELECT * FROM utilisateurs WHERE email = ? AND mot_de_passe = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, motDePasse);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Si un résultat existe, l'authentification est réussie
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'authentification", e);
        }
        return false;
    }

    /**
     * Authentifie un utilisateur par email et mot de passe (retourne l'objet Utilisateur)
     */
    public Utilisateur authentifierUtilisateur(String email, String motDePasse) {
        String sql = "SELECT * FROM utilisateurs WHERE email = ? AND mot_de_passe = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, motDePasse);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Utilisateur utilisateur = new Utilisateur();
                    utilisateur.setId(rs.getInt("id"));
                    utilisateur.setNom(rs.getString("nom"));
                    utilisateur.setPrenom(rs.getString("prenom"));
                    utilisateur.setEmail(rs.getString("email"));
                    utilisateur.setMotDePasse(rs.getString("mot_de_passe"));
                    utilisateur.setRole(rs.getString("role"));
                    return utilisateur;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'authentification", e);
        }
        return null;
    }

    /**
     * Vérifie si un email existe déjà
     */
    public boolean emailExiste(String email) {
        String sql = "SELECT COUNT(*) FROM utilisateurs WHERE email = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la vérification de l'email", e);
        }
        return false;
    }

    /**
     * Change le mot de passe d'un utilisateur
     */
    public boolean changerMotDePasse(String username, String ancienMdp, String nouveauMdp) {
        // Vérifier d'abord que l'ancien mot de passe est correct
        if (!authentifier(username, ancienMdp)) {
            return false;
        }

        String sql = "UPDATE utilisateurs SET mot_de_passe = ? WHERE email = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nouveauMdp);
            pstmt.setString(2, username);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors du changement de mot de passe", e);
        }
        return false;
    }

    /**
     * Met à jour le statut de connexion d'un utilisateur
     */
    public boolean mettreAJourStatutConnexion(String username, boolean estConnecte) {
        String sql = "UPDATE utilisateurs SET est_connecte = ?, derniere_connexion = ? WHERE email = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, estConnecte);
            if (estConnecte) {
                pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            } else {
                pstmt.setNull(2, Types.TIMESTAMP);
            }
            pstmt.setString(3, username);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise à jour du statut de connexion", e);
        }
        return false;
    }

    /**
     * Récupère un utilisateur par son username (email)
     */
    public Utilisateur getUtilisateurByUsername(String username) {
        String sql = "SELECT * FROM utilisateurs WHERE email = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Utilisateur utilisateur = new Utilisateur();
                    utilisateur.setId(rs.getInt("id"));
                    utilisateur.setNom(rs.getString("nom"));
                    utilisateur.setPrenom(rs.getString("prenom"));
                    utilisateur.setEmail(rs.getString("email"));
                    utilisateur.setMotDePasse(rs.getString("mot_de_passe"));
                    utilisateur.setRole(rs.getString("role"));
                    return utilisateur;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'utilisateur", e);
        }
        return null;
    }

    /**
     * Récupère un utilisateur par son ID
     */
    public Utilisateur getUtilisateurById(int id) {
        String sql = "SELECT * FROM utilisateurs WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Utilisateur utilisateur = new Utilisateur();
                    utilisateur.setId(rs.getInt("id"));
                    utilisateur.setNom(rs.getString("nom"));
                    utilisateur.setPrenom(rs.getString("prenom"));
                    utilisateur.setEmail(rs.getString("email"));
                    utilisateur.setMotDePasse(rs.getString("mot_de_passe"));
                    utilisateur.setRole(rs.getString("role"));
                    return utilisateur;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'utilisateur", e);
        }
        return null;
    }

    /**
     * Supprime un utilisateur par son ID
     */
    public boolean supprimerUtilisateur(int id) {
        String sql = "DELETE FROM utilisateurs WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la suppression de l'utilisateur", e);
        }
        return false;
    }
}