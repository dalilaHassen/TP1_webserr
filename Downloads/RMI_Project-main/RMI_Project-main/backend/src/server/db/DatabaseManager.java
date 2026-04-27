package server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;

/**
 * Gestionnaire de connexions MySQL — compatible XAMPP.
 *
 * Lit la configuration depuis db.properties (classpath).
 * Implémente un mini-pool de connexions thread-safe basé sur
 * une connexion partagée (suffisant pour un prototype RMI).
 *
 * En production : remplacer par HikariCP ou c3p0.
 */
public class DatabaseManager {

    // ── Singleton ───────────────────────────────────────────────
    private static DatabaseManager instance;

    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // ── Configuration par défaut (XAMPP) ────────────────────────
    private static final String DEFAULT_URL      = "jdbc:mysql://localhost:3306/gestion_stocks";
    private static final String DEFAULT_USER     = "root";
    private static final String DEFAULT_PASSWORD = "";          // XAMPP : pas de mot de passe par défaut
    private static final String DEFAULT_DRIVER   = "com.mysql.cj.jdbc.Driver";

    private final String url;
    private final String user;
    private final String password;

    // ── Connexion partagée (reconnecte si fermée) ────────────────
    private Connection connection;

    private DatabaseManager() throws SQLException {
        Properties props = chargerConfig();
        this.url      = props.getProperty("db.url",      DEFAULT_URL);
        this.user     = props.getProperty("db.user",     DEFAULT_USER);
        this.password = props.getProperty("db.password", DEFAULT_PASSWORD);

        // Charger le driver JDBC
        try {
            Class.forName(props.getProperty("db.driver", DEFAULT_DRIVER));
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Driver MySQL introuvable. Ajoutez mysql-connector-j-*.jar au classpath.\n" +
                            "Téléchargement : https://dev.mysql.com/downloads/connector/j/", e);
        }

        this.connection = creerConnexion();
        System.out.println("[DB] Connecté à : " + url);
    }

    /**
     * Retourne une connexion valide. Reconnecte automatiquement si nécessaire.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            System.out.println("[DB] Reconnexion à MySQL...");
            connection = creerConnexion();
        }
        return connection;
    }

    private Connection creerConnexion() throws SQLException {
        Properties connProps = new Properties();
        connProps.setProperty("user",                    user);
        connProps.setProperty("password",                password);
        connProps.setProperty("useSSL",                  "false");
        connProps.setProperty("allowPublicKeyRetrieval", "true");
        connProps.setProperty("serverTimezone",          "Europe/Paris");
        connProps.setProperty("characterEncoding",       "UTF-8");
        return DriverManager.getConnection(url, connProps);
    }

    /** Charge db.properties depuis le classpath, revient aux valeurs par défaut sinon. */
    private Properties chargerConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
                System.out.println("[DB] Configuration chargée depuis db.properties");
            } else {
                System.out.println("[DB] db.properties absent — utilisation des valeurs par défaut XAMPP");
            }
        } catch (Exception e) {
            System.err.println("[DB] Erreur lecture db.properties : " + e.getMessage());
        }
        return props;
    }

    public void fermer() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException ignored) {}
    }
}