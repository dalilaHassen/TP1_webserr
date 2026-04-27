package common;

import java.io.Serializable;
import java.security.Timestamp;

public class Utilisateur implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;  // Ajout du username
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String role;
    private boolean estConnecte;
    private Timestamp derniereConnexion;

    // Constructeur par défaut
    public Utilisateur() {}

    // Constructeur original (sans username)
    public Utilisateur(String nom, String prenom, String email, String motDePasse) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = "CHEF_ENTREPOT";
        this.estConnecte = false;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username != null ? username : email; }
    public void setUsername(String username) { this.username = username; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEstConnecte() { return estConnecte; }
    public void setEstConnecte(boolean estConnecte) { this.estConnecte = estConnecte; }

    public Timestamp getDerniereConnexion() { return derniereConnexion; }
    public void setDerniereConnexion(Timestamp derniereConnexion) { this.derniereConnexion = derniereConnexion; }
}