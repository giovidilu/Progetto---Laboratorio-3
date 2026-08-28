package server;

import common.model.User;
import server.repository.GameRepository;
import server.repository.UserRepository;
import server.service.PersistenceManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Test in isolamento (Macro-2, Passo 3) per verificare l'esecuzione dello
 * Shutdown Hook e la persistenza sincrona finale dei dati prima della chiusura della JVM.
 */
public class TestShutdownHookMain {

    private static final String USERS_FILE_PATH = "data/test_shutdown_users.json";
    private static final String GAMES_FILE_PATH = "data/test_shutdown_games.json";

    public static void main(String[] args) {
        String mode = (args.length > 0) ? args[0] : "run";

        if ("verify".equalsIgnoreCase(mode)) {
            verifyPersistedData();
        } else {
            runShutdownSimulation();
        }
    }

    /**
     * Fase 1: Inizializza i componenti, registra lo shutdown hook,
     * inserisce dati in memoria e termina forzatamente la JVM con System.exit(0).
     */
    private static void runShutdownSimulation() {
        System.out.println("=== FASE 1: Simulazione Shutdown Hook ===");

        // Pulizia preliminare dei file di test precedenti
        new File(USERS_FILE_PATH).delete();
        new File(GAMES_FILE_PATH).delete();

        try {
            UserRepository userRepo = new UserRepository(USERS_FILE_PATH);
            GameRepository gameRepo = new GameRepository(GAMES_FILE_PATH);

            // Intervallo lungo (60 minuti) per escludere salvataggi periodici intermedi
            PersistenceManager persistenceManager = new PersistenceManager(
                userRepo,
                gameRepo,
                60,
                TimeUnit.MINUTES
            );

            // Registrazione dello Shutdown Hook identico a quello di ServerMain
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SHUTDOWN HOOK] Segnale di arresto intercettato!");
                System.out.println("[SHUTDOWN HOOK] Esecuzione flush finale sincrono su disco...");
                persistenceManager.stop();
                System.out.println("[SHUTDOWN HOOK] Chiusura completata con successo.");
            }));

            // Avvio dello scheduler
            persistenceManager.start();
            System.out.println("[1] PersistenceManager avviato con successo.");

            // Inserimento utente solo in memoria (nessun salvataggio manuale invocato)
            System.out.println("[2] Inserimento 'utente_hook' nella memoria volatile...");
            userRepo.addUser(new User("utente_hook", "hash_hook_xyz", "salt_hook_123"));

            System.out.println("[3] Invocazione di System.exit(0) per forzare lo spegnimento della JVM...");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("[ERRORE] Errore imprevisto durante la simulazione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fase 2: Ricarica i dati da disco con una nuova istanza del repository
     * per verificare che lo shutdown hook abbia effettivamente scritto su file.
     */
    private static void verifyPersistedData() {
        System.out.println("=== FASE 2: Verifica Persistenza su Disco ===");

        try {
            UserRepository repoVerifier = new UserRepository(USERS_FILE_PATH);
            repoVerifier.loadFromDisk();

            User savedUser = repoVerifier.getUser("utente_hook");

            if (savedUser != null && "hash_hook_xyz".equals(savedUser.getPasswordHash())) {
                System.out.println("[SUCCESSO] 'utente_hook' è presente su disco con i dati corretti!");
                System.out.println("[ESITO] Lo Shutdown Hook e il flush sincrono finale funzionano correttamente.");
            } else {
                System.err.println("[FALLITO] I dati non sono stati salvati su disco durante lo spegnimento.");
            }

        } catch (IOException e) {
            System.err.println("[ERRORE I/O] Impossibile leggere il file di persistenza: " + e.getMessage());
        } finally {
            // Pulizia finale dei file generati per il test
            new File(USERS_FILE_PATH).delete();
            new File(GAMES_FILE_PATH).delete();
            System.out.println("\nFile di test rimossi.");
        }
    }
}