package common;

import java.io.Serializable;

public class Produit implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String nom;
    private final String categorie;
    private final String description;



    private final double prixUnitaire;

    public Produit(String id, String nom, String categorie,String description, double prixUnitaire) {
        this.id           = id;
        this.nom          = nom;
        this.categorie    = categorie;
        this.description = description;

        this.prixUnitaire = prixUnitaire;
    }

    public String getId()           { return id; }
    public String getNom()          { return nom; }
    public String getCategorie()    { return categorie; }
    public String getDescription()    { return description; }

    public double getPrixUnitaire() { return prixUnitaire; }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) — %.2f €", id, nom, categorie, prixUnitaire);
    }
}