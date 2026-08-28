package server;

import common.model.User;
import server.repository.GameRepository;
import server.repository.UserRepository;
import server.service.PersistenceManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Test in isolamento per verificare il salvataggio periodico asincrono
 * gestito da PersistenceManager tramite ScheduledExecutorService.
 */
public class TestPersistenceManagerMain {

    public static void main(String[] args) {
        String testUsersPath = "data/test_users_periodic.json";
        String testGamesPath = "data/test_games_periodic.json";

        File usersFile = new File(testUsersPath);
        File gamesFile = new File(testGamesPath);

        // Pulizia preliminare dei file temporanei
        if (usersFile.exists()) usersFile.delete();
        if (gamesFile.exists()) gamesFile.delete();

        System.out.println("=== INIZIO TEST ISOLATO: PersistenceManager (Periodicità) ===");

        try {
            // 1. Inizializzazione repository e PersistenceManager con intervallo rapido (2 secondi)
            UserRepository userRepo = new UserRepository(testUsersPath);
            GameRepository gameRepo = new GameRepository(testGamesPath);

            long testInterval = 2;
            PersistenceManager persistenceManager = new PersistenceManager(
                userRepo,
                gameRepo,
                testInterval,
                TimeUnit.SECONDS
            );

            System.out.println("\n[1] Avvio di PersistenceManager (intervallo: " + testInterval + " secondi)...");
            persistenceManager.start();

            // 2. Inserimento primo utente in memoria
            System.out.println("\n[2] Inserimento 'utente_periodico_1' in memoria...");
            userRepo.addUser(new User("utente_periodico_1", "hash1", "salt1"));

            System.out.println("Attesa di 3.5 secondi per consentire il flush periodico automatico...");
            Thread.sleep(3500);

            // 3. Verifica del primo salvataggio su disco tramite seconda istanza
            System.out.println("\n[3] Verifica primo salvataggio su disco...");
            UserRepository repoVerifier1 = new UserRepository(testUsersPath);
            repoVerifier1.loadFromDisk();

            User user1Loaded = repoVerifier1.getUser("utente_periodico_1");
            if (user1Loaded != null) {
                System.out.println(" -> OK: 'utente_periodico_1' trovato su disco senza chiamate manuali!");
            } else {
                System.err.println(" -> FALLITO: Il file su disco non è stato aggiornato dallo scheduler.");
                persistenceManager.stop();
                return;
            }

            // 4. Inserimento secondo utente in memoria
            System.out.println("\n[4] Inserimento 'utente_periodico_2' in memoria...");
            userRepo.addUser(new User("utente_periodico_2", "hash2", "salt2"));

            System.out.println("Attesa di ulteriori 3.5 secondi per il ciclo successivo...");
            Thread.sleep(3500);

            // 5. Verifica del secondo salvataggio su disco
            System.out.println("\n[5] Verifica secondo salvataggio su disco...");
            UserRepository repoVerifier2 = new UserRepository(testUsersPath);
            repoVerifier2.loadFromDisk();

            User user2Loaded = repoVerifier2.getUser("utente_periodico_2");
            if (user2Loaded != null) {
                System.out.println(" -> OK: 'utente_periodico_2' trovato su disco!");
                System.out.println("\n[SUCCESSO] Il salvataggio periodico di PersistenceManager funziona correttamente!");
            } else {
                System.err.println(" -> FALLITO: Il secondo flush periodico non ha scritto i nuovi dati.");
            }

            // 6. Arresto controllato
            System.out.println("\n[6] Arresto di PersistenceManager...");
            persistenceManager.stop();

        } catch (InterruptedException e) {
            System.err.println("[ERRORE] Test interrotto durante l'attesa: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("[ERRORE I/O] Errore di lettura/scrittura: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Pulizia finale dei file generati per il test
            if (usersFile.exists()) usersFile.delete();
            if (gamesFile.exists()) gamesFile.delete();
            System.out.println("\nPulizia file di test completata.");
        }
    }
}