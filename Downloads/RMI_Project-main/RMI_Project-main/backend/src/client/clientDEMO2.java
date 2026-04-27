package client;

import common.IAuthentification;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Client de test pour l'authentification (Sign In / Sign Up)
 *
 * Utilisation: java client.clientDEMO2 [host] [port]
 * Valeurs par défaut: localhost:1099
 */
public class clientDEMO2 {

    private static IAuthentification authService;
    private static String currentUser = null;

    public static void main(String[] args) {
        try {
            // Connexion au registre RMI
            String host = args.length > 0 ? args[0] : "localhost";
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 1099;

            Registry registry = LocateRegistry.getRegistry(host, port);

            // ✅ CORRECTION : Utiliser le bon nom "Authentification" au lieu de "GestionStocks"
            IAuthentification authService = (IAuthentification) registry.lookup("Authentification");

            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║   SYSTÈME D'AUTHENTIFICATION RMI     ║");
            System.out.println("╚══════════════════════════════════════╝\n");

            Scanner scanner = new Scanner(System.in);
            boolean quit = false;

            while (!quit) {
                if (currentUser == null) {
                    // Menu non connecté
                    System.out.println("┌────────────────────────────────────┐");
                    System.out.println("│ 1. 📝 Créer un compte (Sign Up)    │");
                    System.out.println("│ 2. 🔐 Se connecter (Sign In)       │");
                    System.out.println("│ 3. ❌ Quitter                       │");
                    System.out.println("└────────────────────────────────────┘");
                    System.out.print("Votre choix: ");

                    int choix = scanner.nextInt();
                    scanner.nextLine();

                    switch (choix) {
                        case 1:
                            signUp(scanner, authService);
                            break;
                        case 2:
                            signIn(scanner, authService);
                            break;
                        case 3:
                            quit = true;
                            break;
                        default:
                            System.out.println("❌ Choix invalide !\n");
                    }
                } else {
                    // Menu connecté
                    System.out.println("\n┌────────────────────────────────────┐");
                    System.out.println("│   👋 Connecté en tant que :        │");
                    System.out.println("│      " + currentUser);
                    System.out.println("├────────────────────────────────────┤");
                    System.out.println("│ 1. 👤 Voir mon profil              │");
                    System.out.println("│ 2. 🔑 Changer mon mot de passe     │");
                    System.out.println("│ 3. 🚪 Se déconnecter (Sign Out)    │");
                    System.out.println("│ 4. 🚀 Quitter                       │");
                    System.out.println("└────────────────────────────────────┘");
                    System.out.print("Votre choix: ");

                    int choix = scanner.nextInt();
                    scanner.nextLine();

                    switch (choix) {
                        case 1:
                            voirProfil(authService);
                            break;
                        case 2:
                            changerMotDePasse(scanner, authService);
                            break;
                        case 3:
                            signOut(authService);
                            break;
                        case 4:
                            quit = true;
                            break;
                        default:
                            System.out.println("❌ Choix invalide !\n");
                    }
                }
            }

            scanner.close();
            System.out.println("\n👋 Au revoir !");

        } catch (Exception e) {
            System.err.println("\n❌ Erreur de connexion au serveur: " + e.getMessage());
            System.err.println("   Vérifiez que le serveur RMI est démarré.");
            System.err.println("   Assurez-vous que le service 'Authentification' est publié.\n");
            e.printStackTrace();
        }
    }

    /**
     * Création d'un nouveau compte (Sign Up)
     */
    private static void signUp(Scanner scanner, IAuthentification authService) {
        try {
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║       CRÉATION DE COMPTE (SIGN UP)    ║");
            System.out.println("╚══════════════════════════════════════╝\n");

            System.out.print("📧 Email (servira d'identifiant): ");
            String email = scanner.nextLine();

            System.out.print("🔒 Mot de passe (min 4 caractères): ");
            String password = scanner.nextLine();

            System.out.print("👤 Nom complet: ");
            String nom = scanner.nextLine();

            // Validation simple
            if (email.trim().isEmpty() || password.trim().isEmpty() || nom.trim().isEmpty()) {
                System.out.println("\n❌ Tous les champs sont obligatoires !\n");
                return;
            }

            if (password.length() < 4) {
                System.out.println("\n❌ Le mot de passe doit contenir au moins 4 caractères !\n");
                return;
            }

            System.out.println("\n⏳ Création du compte en cours...");

            boolean success = authService.creerCompte(email, password, nom, email);
            System.out.println(success);
            if (success) {
                System.out.println("\n✅ COMPTE CRÉÉ AVEC SUCCÈS !");
                System.out.println("   Vous pouvez maintenant vous connecter.\n");
            } else {
                System.out.println("\n❌ ÉCHEC DE LA CRÉATION DU COMPTE");
                System.out.println("   L'adresse email existe peut-être déjà.\n");
            }

        } catch (Exception e) {
            System.err.println("\n❌ Erreur lors de la création: " + e.getMessage() + "\n");
        }
    }

    /**
     * Connexion d'un utilisateur (Sign In)
     */
    private static void signIn(Scanner scanner, IAuthentification authService) {
        try {
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║           CONNEXION (SIGN IN)         ║");
            System.out.println("╚══════════════════════════════════════╝\n");

            System.out.print("📧 Email: ");
            String email = scanner.nextLine();

            System.out.print("🔒 Mot de passe: ");
            String password = scanner.nextLine();

            System.out.println("\n⏳ Authentification en cours...");

            boolean success = authService.seConnecter(email, password);

            if (success) {
                currentUser = email;
                System.out.println("\n✅ CONNEXION RÉUSSIE !");
                System.out.println("   Bienvenue " + email + " !\n");
            } else {
                System.out.println("\n❌ ÉCHEC DE CONNEXION");
                System.out.println("   Email ou mot de passe incorrect.\n");
            }

        } catch (Exception e) {
            System.err.println("\n❌ Erreur lors de la connexion: " + e.getMessage() + "\n");
        }
    }

    /**
     * Déconnexion (Sign Out)
     */
    private static void signOut(IAuthentification authService) {
        try {
            authService.seDeconnecter(currentUser);
            System.out.println("\n✅ DÉCONNEXION RÉUSSIE !");
            System.out.println("   À bientôt " + currentUser + " !\n");
            currentUser = null;
        } catch (Exception e) {
            System.err.println("\n❌ Erreur lors de la déconnexion: " + e.getMessage() + "\n");
        }
    }

    /**
     * Afficher le profil de l'utilisateur connecté
     */
    private static void voirProfil(IAuthentification authService) {
        try {
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║              MON PROFIL               ║");
            System.out.println("╚══════════════════════════════════════╝\n");
            System.out.println("📧 Email: " + currentUser);
            System.out.println("👤 Statut: Connecté(e)");
            System.out.println("🔐 Sécurité: Active");

            // Vérifier l'état de connexion
            boolean isConnected = authService.estConnecte(currentUser);
            if (isConnected) {
                System.out.println("✅ Session active");
            } else {
                System.out.println("⚠️ Session inactive");
            }

            System.out.println("\n🎯 Rôle: Chef d'entrepôt");
            System.out.println();

        } catch (Exception e) {
            System.err.println("\n❌ Erreur: " + e.getMessage() + "\n");
        }
    }

    /**
     * Changer le mot de passe
     */
    private static void changerMotDePasse(Scanner scanner, IAuthentification authService) {
        try {
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║        CHANGEMENT DE MOT DE PASSE     ║");
            System.out.println("╚══════════════════════════════════════╝\n");

            System.out.print("🔒 Ancien mot de passe: ");
            String ancienMdp = scanner.nextLine();

            System.out.print("🔑 Nouveau mot de passe (min 4 caractères): ");
            String nouveauMdp = scanner.nextLine();

            System.out.print("🔑 Confirmer le nouveau mot de passe: ");
            String confirmeMdp = scanner.nextLine();

            if (!nouveauMdp.equals(confirmeMdp)) {
                System.out.println("\n❌ Les mots de passe ne correspondent pas !\n");
                return;
            }

            if (nouveauMdp.length() < 4) {
                System.out.println("\n❌ Le mot de passe doit contenir au moins 4 caractères !\n");
                return;
            }

            System.out.println("\n⏳ Modification en cours...");

            boolean success = authService.changerMotDePasse(currentUser, ancienMdp, nouveauMdp);

            if (success) {
                System.out.println("\n✅ MOT DE PASSE CHANGÉ AVEC SUCCÈS !\n");
            } else {
                System.out.println("\n❌ ÉCHEC DU CHANGEMENT");
                System.out.println("   Vérifiez votre ancien mot de passe.\n");
            }

        } catch (Exception e) {
            System.err.println("\n❌ Erreur: " + e.getMessage() + "\n");
        }
    }
}