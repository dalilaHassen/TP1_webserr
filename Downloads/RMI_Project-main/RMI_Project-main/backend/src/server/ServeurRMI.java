package server;

import common.IAuthentification;
import common.IGestionStocks;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServeurRMI {
    private static final Logger logger = Logger.getLogger(ServeurRMI.class.getName());

    public static void main(String[] args) {
        try {
            // Création des objets distants
            GestionStocksImpl gestionStocks = new GestionStocksImpl();      // ← type concret
            AuthentificationImpl authentification = new AuthentificationImpl(); // ← type concret

            // Démarrage du registre RMI sur le port 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            // Publication des objets distants
            registry.rebind("GestionStocks", gestionStocks);
            registry.rebind("Authentification", authentification);

            logger.info("✅ Serveur RMI démarré sur le port 1099");
            logger.info("✅ Service GestionStocks publié");
            logger.info("✅ Service Authentification publié");

            // Démarrage du bridge HTTP
            HttpBridge bridge = new HttpBridge(authentification);
            bridge.start();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Erreur lors du démarrage du serveur", e);
        }
    }
}