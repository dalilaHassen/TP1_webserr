package server;

import common.IAuthentification;
import common.IGestionStocks;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServeurRMI {
    private static final Logger logger = Logger.getLogger(ServeurRMI.class.getName());

    public static void main(String[] args) {
        try {
            // Création des objets distants
            // GestionStocksImpl et AuthentificationImpl étendent UnicastRemoteObject :
            // l'export RMI est déjà fait automatiquement dans leur constructeur.
            // Ne PAS appeler exportObject() une deuxième fois.
            GestionStocksImpl    gestionStocks   = new GestionStocksImpl();
            AuthentificationImpl authentification = new AuthentificationImpl();

            // Démarrage du registre RMI sur le port 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            // Publication dans le registre (on passe l'objet directement, pas un stub)
            registry.rebind("GestionStocks",    gestionStocks);
            registry.rebind("Authentification", authentification);

            logger.info("✅ Serveur RMI démarré sur le port 1099");
            logger.info("✅ Service GestionStocks publié");
            logger.info("✅ Service Authentification publié");

            // Démarrage du bridge HTTP
            try {
                HttpBridge bridge = new HttpBridge(authentification, gestionStocks);
                bridge.start();
                logger.info("✅ HTTP Bridge démarré sur http://localhost:8080");
            } catch (Exception e) {
                logger.warning("⚠️ HTTP Bridge non démarré : " + e.getMessage());
            }

        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "❌ Erreur RMI lors du démarrage du serveur", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Erreur lors du démarrage du serveur", e);
        }
    }
}