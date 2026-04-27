package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuthentification extends Remote {

    /**
     * Crée un nouveau compte
     * @param username Nom d'utilisateur (email)
     * @param password Mot de passe
     * @param nom Nom complet
     * @param email Email
     * @return true si succès
     */
    boolean creerCompte(String username, String password, String nom, String email) throws RemoteException;

    /**
     * Connecte un utilisateur
     * @param username Nom d'utilisateur (email)
     * @param password Mot de passe
     * @return true si authentifié
     */
    boolean seConnecter(String username, String password) throws RemoteException;

    /**
     * Déconnecte un utilisateur
     * @param username Nom d'utilisateur
     */
    void seDeconnecter(String username) throws RemoteException;

    /**
     * Vérifie si un utilisateur est connecté
     * @param username Nom d'utilisateur
     * @return true si connecté
     */
    boolean estConnecte(String username) throws RemoteException;

    /**
     * Change le mot de passe
     * @param username Nom d'utilisateur
     * @param ancienMdp Ancien mot de passe
     * @param nouveauMdp Nouveau mot de passe
     * @return true si succès
     */
    boolean changerMotDePasse(String username, String ancienMdp, String nouveauMdp) throws RemoteException;
}