package server;

import common.*;
import server.db.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Implémentation RMI du service de gestion des stocks — version MySQL/JDBC.
 *
 * Les données sont persistées dans MySQL/MariaDB via des DAOs JDBC.
 * Les opérations critiques (sortie, transfert) utilisent des transactions
 * explicites avec verrou de ligne (SELECT … FOR UPDATE) pour garantir
 * la cohérence en environnement multi-clients.
 */
public class GestionStocksImpl extends UnicastRemoteObject implements IGestionStocks {

    private static final long serialVersionUID = 1L;

    private final DatabaseManager dbManager;
    private final ProduitDAO      produitDAO;
    private final EntrepotDAO     entrepotDAO;
    private final StockDAO        stockDAO;
    private final MouvementDAO    mouvementDAO;

    // ────────────────────────────────────────────────────────
    public GestionStocksImpl() throws RemoteException {
        super();
        try {
            this.dbManager    = DatabaseManager.getInstance();
            this.produitDAO   = new ProduitDAO(dbManager);
            this.entrepotDAO  = new EntrepotDAO(dbManager);
            this.stockDAO     = new StockDAO(dbManager);
            this.mouvementDAO = new MouvementDAO(dbManager);
        } catch (SQLException e) {
            throw new RemoteException("Impossible de se connecter à la BD : " + e.getMessage(), e);
        }
    }

    // ═══════════════════ PRODUITS ════════════════════════════

    @Override
    public void ajouterProduit(Produit produit) throws RemoteException {
        try {
            if (produitDAO.existe(produit.getId()))
                throw new RemoteException("Produit déjà existant : " + produit.getId());
            produitDAO.inserer(produit);
            log("PRODUIT AJOUTÉ", produit.toString());
        } catch (SQLException e) { jdbcErreur(e); }
    }

    @Override
    public List<Produit> listerProduits() throws RemoteException {
        try {
            return produitDAO.listerTous();
        } catch (SQLException e) { throw jdbcErreur(e); }
    }

    @Override
    public Produit getProduit(String idProduit) throws RemoteException {
        try {
            Produit p = produitDAO.trouverParId(idProduit);
            if (p == null) throw new RemoteException("Produit inconnu : " + idProduit);
            return p;
        } catch (SQLException e) { throw jdbcErreur(e); }
    }

    // ═══════════════════ ENTREPÔTS ═══════════════════════════

    @Override
    public void ajouterEntrepot(String id, String nom, String ville) throws RemoteException {
        try {
            if (entrepotDAO.existe(id))
                throw new RemoteException("Entrepôt déjà existant : " + id);
            entrepotDAO.inserer(id, nom, ville);
            log("ENTREPÔT AJOUTÉ", id + " → " + nom + " (" + ville + ")");
        } catch (SQLException e) { jdbcErreur(e); }
    }

    @Override
    public Map<String, String> listerEntrepots() throws RemoteException {
        try {
            return entrepotDAO.listerTous();
        } catch (SQLException e) { throw jdbcErreur(e); }
    }

    // ═══════════════════ STOCKS ══════════════════════════════

    @Override
    public void entreeStock(String idEntrepot, String idProduit,
                            int quantite, String note) throws RemoteException {
        validerEntite(idEntrepot, idProduit, quantite);
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            stockDAO.ajusterQuantite(conn, idEntrepot, idProduit, quantite);

            MouvementStock mouv = new MouvementStock(
                    idProduit, idEntrepot,
                    MouvementStock.TypeMouvement.ENTREE, quantite, note);
            mouvementDAO.inserer(conn, mouv);

            conn.commit();
            log("ENTRÉE STOCK", mouv.toString());

        } catch (SQLException e) {
            rollback(conn);
            throw jdbcErreur(e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public void sortieStock(String idEntrepot, String idProduit,
                            int quantite, String note) throws RemoteException {
        validerEntite(idEntrepot, idProduit, quantite);
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            // Verrou de ligne pour éviter les conditions de course
            int actuel = getStockForUpdate(conn, idEntrepot, idProduit);
            if (actuel < quantite)
                throw new RemoteException(
                        String.format("Stock insuffisant [%s/%s] : disponible=%d, demandé=%d",
                                idEntrepot, idProduit, actuel, quantite));

            stockDAO.ajusterQuantite(conn, idEntrepot, idProduit, -quantite);

            MouvementStock mouv = new MouvementStock(
                    idProduit, idEntrepot,
                    MouvementStock.TypeMouvement.SORTIE, quantite, note);
            mouvementDAO.inserer(conn, mouv);

            conn.commit();
            log("SORTIE STOCK", mouv.toString());

        } catch (SQLException e) {
            rollback(conn);
            throw jdbcErreur(e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public void transfererStock(String idSrc, String idDest,
                                String idProduit, int quantite) throws RemoteException {
        try {
            validerExistenceEntrepot(idSrc);
            validerExistenceEntrepot(idDest);
            validerExistenceProduit(idProduit);
            validerQuantitePositive(quantite);
        } catch (SQLException e) { throw jdbcErreur(e); }

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            int stockSrc = getStockForUpdate(conn, idSrc, idProduit);
            if (stockSrc < quantite)
                throw new RemoteException(
                        String.format("Stock insuffisant pour transfert [%s/%s] : dispo=%d, demandé=%d",
                                idSrc, idProduit, stockSrc, quantite));

            stockDAO.ajusterQuantite(conn, idSrc,  idProduit, -quantite);
            stockDAO.ajusterQuantite(conn, idDest, idProduit, +quantite);

            String noteT = "Transfert de " + idSrc + " vers " + idDest;
            mouvementDAO.inserer(conn, new MouvementStock(
                    idProduit, idSrc,
                    MouvementStock.TypeMouvement.TRANSFERT, quantite, noteT + " [sortie]"));
            mouvementDAO.inserer(conn, new MouvementStock(
                    idProduit, idDest,
                    MouvementStock.TypeMouvement.TRANSFERT, quantite, noteT + " [entrée]"));

            conn.commit();
            log("TRANSFERT", String.format("%s → %s | %s | qté: %d",
                    idSrc, idDest, idProduit, quantite));

        } catch (SQLException e) {
            rollback(conn);
            throw jdbcErreur(e);
        } finally {
            resetAutoCommit(conn);
        }
    }

    @Override
    public int getStock(String idEntrepot, String idProduit) throws RemoteException {
        try {
            validerExistenceEntrepot(idEntrepot);
            validerExistenceProduit(idProduit);
            return stockDAO.getQuantite(idEntrepot, idProduit);
        } catch (SQLException e) { throw jdbcErreur(e); }
    }

    @Override
    public Map<String, Integer> getStockEntrepot(String idEntrepot) throws RemoteException {
        try {
            validerExistenceEntrepot(idEntrepot);
            return stockDAO.getStocksEntrepot(idEntrepot);
        } catch (SQLException e) { throw jdbcErreur(e); }
    }

    @Override
    public Map<String, Integer> getStockProduitTous(String idProduit) throws RemoteException {
        try {
            validerExistenceProduit(idProduit);
            return stockDAO.getStocksProduitTousEntrepots(idProduit);
        } catch (SQLException e) { throw jdbcErreur(e); }
    }

    @Override
    public List<String> alertesStockBas(int seuilMinimal) throws RemoteException {
        try {
            return stockDAO.getProduitsSousSeuilGlobal(seuilMinimal);
        } catch (SQLException e) { throw jdbcErreur(e); }
    }

    // ═══════════════════ HISTORIQUE ══════════════════════════

    @Override
    public List<MouvementStock> getHistoriqueEntrepot(String idEntrepot) throws RemoteException {
        try {
            validerExistenceEntrepot(idEntrepot);
            return mouvementDAO.listerParEntrepot(idEntrepot);
        } catch (SQLException e) { throw jdbcErreur(e); }
    }

    @Override
    public List<MouvementStock> getHistoriqueProduit(String idProduit) throws RemoteException {
        try {
            validerExistenceProduit(idProduit);
            return mouvementDAO.listerParProduit(idProduit);
        } catch (SQLException e) { throw jdbcErreur(e); }
    }

    // ═══════════════════ UTILITAIRES PRIVÉS ══════════════════

    /** Verrou de ligne pour les lectures précédant une écriture. */
    private int getStockForUpdate(Connection conn,
                                  String idEntrepot, String idProduit) throws SQLException {
        String sql = "SELECT quantite FROM stocks " +
                "WHERE id_entrepot = ? AND id_produit = ? FOR UPDATE";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, idEntrepot);
            ps.setString(2, idProduit);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("quantite") : 0;
            }
        }
    }

    private void validerEntite(String idEntrepot, String idProduit,
                               int quantite) throws RemoteException {
        try {
            validerExistenceEntrepot(idEntrepot);
            validerExistenceProduit(idProduit);
            validerQuantitePositive(quantite);
        } catch (SQLException e) { throw jdbcErreur(e); }
    }

    private void validerExistenceEntrepot(String id) throws SQLException, RemoteException {
        if (!entrepotDAO.existe(id))
            throw new RemoteException("Entrepôt inconnu : " + id);
    }

    private void validerExistenceProduit(String id) throws SQLException, RemoteException {
        if (!produitDAO.existe(id))
            throw new RemoteException("Produit inconnu : " + id);
    }

    private void validerQuantitePositive(int q) throws RemoteException {
        if (q <= 0)
            throw new RemoteException("La quantité doit être > 0, reçu : " + q);
    }

    private void rollback(Connection conn) {
        if (conn != null) try { conn.rollback(); }
        catch (SQLException ignored) {}
    }

    private void resetAutoCommit(Connection conn) {
        if (conn != null) try { conn.setAutoCommit(true); }
        catch (SQLException ignored) {}
    }

    private RemoteException jdbcErreur(SQLException e) throws RemoteException {
        System.err.println("[DB ERROR] " + e.getMessage());
        throw new RemoteException("Erreur base de données : " + e.getMessage(), e);
    }

    private void log(String action, String detail) {
        System.out.printf("[SERVER] %-20s %s%n", action, detail);
    }
}