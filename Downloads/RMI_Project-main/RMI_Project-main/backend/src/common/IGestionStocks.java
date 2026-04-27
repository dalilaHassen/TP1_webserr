package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface IGestionStocks extends Remote {

    // ── Produits ─────────────────────────────────────────────────
    void         ajouterProduit(Produit produit)       throws RemoteException;
    List<Produit> listerProduits()                     throws RemoteException;
    Produit      getProduit(String idProduit)          throws RemoteException;

    // ── Entrepôts ─────────────────────────────────────────────────
    void                ajouterEntrepot(String id, String nom, String ville) throws RemoteException;
    Map<String, String> listerEntrepots()                                    throws RemoteException;

    // ── Stocks ────────────────────────────────────────────────────
    void entreeStock(String idEntrepot, String idProduit,
                     int quantite, String note)              throws RemoteException;

    void sortieStock(String idEntrepot, String idProduit,
                     int quantite, String note)              throws RemoteException;

    void transfererStock(String idEntrepotSource, String idEntrepotDest,
                         String idProduit, int quantite)     throws RemoteException;

    int                  getStock(String idEntrepot, String idProduit) throws RemoteException;
    Map<String, Integer> getStockEntrepot(String idEntrepot)           throws RemoteException;
    Map<String, Integer> getStockProduitTous(String idProduit)         throws RemoteException;
    List<String>         alertesStockBas(int seuilMinimal)             throws RemoteException;

    // ── Historique ────────────────────────────────────────────────
    List<MouvementStock> getHistoriqueEntrepot(String idEntrepot) throws RemoteException;
    List<MouvementStock> getHistoriqueProduit(String idProduit)   throws RemoteException;
}