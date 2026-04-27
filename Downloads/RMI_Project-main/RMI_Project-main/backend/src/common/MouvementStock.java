package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MouvementStock implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum TypeMouvement { ENTREE, SORTIE, TRANSFERT }

    private final String        idProduit;
    private final String        idEntrepot;
    private final TypeMouvement type;
    private final int           quantite;
    private final LocalDateTime dateHeure;
    private final String        note;

    public MouvementStock(String idProduit, String idEntrepot,
                          TypeMouvement type, int quantite, String note) {
        this.idProduit  = idProduit;
        this.idEntrepot = idEntrepot;
        this.type       = type;
        this.quantite   = quantite;
        this.note       = note;
        this.dateHeure  = LocalDateTime.now();
    }

    // Constructeur pour reconstitution depuis la BD (date explicite)
    public MouvementStock(String idProduit, String idEntrepot,
                          TypeMouvement type, int quantite,
                          String note, LocalDateTime dateHeure) {
        this.idProduit  = idProduit;
        this.idEntrepot = idEntrepot;
        this.type       = type;
        this.quantite   = quantite;
        this.note       = note;
        this.dateHeure  = dateHeure;
    }

    public String        getIdProduit()  { return idProduit; }
    public String        getIdEntrepot() { return idEntrepot; }
    public TypeMouvement getType()       { return type; }
    public int           getQuantite()   { return quantite; }
    public LocalDateTime getDateHeure()  { return dateHeure; }
    public String        getNote()       { return note; }

    @Override
    public String toString() {
        return String.format("%s | %-4s | %-4s | %9s | qté: %4d | %s",
                dateHeure, idEntrepot, idProduit, type, quantite, note);
    }
}