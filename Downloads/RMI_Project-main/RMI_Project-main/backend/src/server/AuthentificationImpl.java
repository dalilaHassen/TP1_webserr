// src/server/AuthentificationImpl.java
package server;

import common.IAuthentification;
import server.db.UtilisateurDAO;
import common.Utilisateur;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthentificationImpl extends UnicastRemoteObject implements IAuthentification {
    private static final long serialVersionUID = 1L;

    private UtilisateurDAO utilisateurDAO;
    private ConcurrentHashMap<String, Boolean> sessions; // Pour suivre les sessions actives

    public AuthentificationImpl() throws RemoteException {
        super();
        this.utilisateurDAO = new UtilisateurDAO();
        this.sessions = new ConcurrentHashMap<>();


    }



    @Override
    public boolean creerCompte(String username, String password, String nom, String email) throws RemoteException {
        System.out.println("=== CRÉATION DE COMPTE ===");
        System.out.println("Username: " + username);
        System.out.println("Nom: " + nom);
        System.out.println("Email: " + email);
        System.out.println("Password length: " + (password != null ? password.length() : 0));

        try {
            // Validation des entrées
            if (username == null || username.trim().isEmpty() ||
                    password == null || password.trim().isEmpty() ||
                    nom == null || nom.trim().isEmpty() ||
                    email == null || email.trim().isEmpty()) {
                System.out.println("❌ Champs manquants");
                return false;
            }

            if (password.length() < 4) {
                System.out.println("❌ Mot de passe trop court");
                return false;
            }

            // Vérifier si l'email existe déjà
            System.out.println("Vérification email existant: " + email);
            if (utilisateurDAO.emailExiste(email)) {
                System.out.println("❌ Email déjà utilisé: " + email);
                return false;
            }

            // Créer l'utilisateur
            Utilisateur nouvelUtilisateur = new Utilisateur();
            nouvelUtilisateur.setEmail(email);
            nouvelUtilisateur.setMotDePasse(password);
            nouvelUtilisateur.setNom(nom);
            nouvelUtilisateur.setPrenom("");
            nouvelUtilisateur.setRole("CHEF_ENTREPOT");

            System.out.println("Tentative d'insertion dans la base de données...");
            boolean success = utilisateurDAO.ajouterUtilisateur(nouvelUtilisateur);

            if (success) {
                System.out.println("✅ Compte créé avec succès! ID: " + nouvelUtilisateur.getId());
                return true;
            } else {
                System.out.println("❌ Échec de l'insertion - retour false");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }    @Override

    public boolean seConnecter(String username, String password) throws RemoteException {
        if (username == null || password == null) return false;

        // ✅ Si déjà connecté, on réinitialise la session
        if (estConnecte(username)) {
            sessions.remove(username);
        }

        boolean authenticated = utilisateurDAO.authentifier(username, password);

        if (authenticated) {
            sessions.put(username, true);
            utilisateurDAO.mettreAJourStatutConnexion(username, true);
            System.out.println("Utilisateur connecté: " + username);
        }

        return authenticated;
    }
    @Override
    public void seDeconnecter(String username) throws RemoteException {
        if (username != null) {
            sessions.remove(username);
            utilisateurDAO.mettreAJourStatutConnexion(username, false);
            System.out.println("Utilisateur déconnecté: " + username);
        }
    }

    @Override
    public boolean estConnecte(String username) throws RemoteException {
        return sessions.containsKey(username) && sessions.get(username);
    }

    @Override
    public boolean changerMotDePasse(String username, String ancienMdp, String nouveauMdp) throws RemoteException {
        if (username == null || ancienMdp == null || nouveauMdp == null) {
            return false;
        }

        if (nouveauMdp.trim().length() < 4) {
            System.err.println("Le nouveau mot de passe doit contenir au moins 4 caractères");
            return false;
        }

        boolean changed = utilisateurDAO.changerMotDePasse(username, ancienMdp, nouveauMdp);

        if (changed) {
            System.out.println("Mot de passe changé pour: " + username);
        }

        return changed;
    }
}