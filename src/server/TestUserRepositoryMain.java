package server;

import common.model.User;
import server.repository.UserRepository;

import java.io.File;
import java.io.IOException;

/**
 * Test in isolamento (Macro-2) per verificare il ciclo di serializzazione
 * e deserializzazione su disco di UserRepository.
 */
public class TestUserRepositoryMain {

    public static void main(String[] args) {
        String testFilePath = "data/test_users.json";

        // Pulizia preliminare dell'eventuale file di test precedente
        File testFile = new File(testFilePath);
        if (testFile.exists()) {
            testFile.delete();
        }

        System.out.println("=== INIZIO TEST ISOLATO: UserRepository ===");

        try {
            // FASE 1: Scrittura con la prima istanza del repository
            System.out.println("\n[1] Creazione prima istanza di UserRepository...");
            UserRepository repoWrite = new UserRepository(testFilePath);

            // Creazione di un utente di test con credenziali simulate
            User user1 = new User("test_giovanni", "hash_simulato_123", "salt_simulato_456");
            boolean added = repoWrite.addUser(user1);
            System.out.println("Utente aggiunto in memoria: " + added);

            System.out.println("Salvataggio su disco in " + testFilePath + "...");
            repoWrite.saveToDisk();
            System.out.println("Salvataggio completato.");

            // FASE 2: Lettura indipendente con una seconda istanza del repository
            System.out.println("\n[2] Creazione seconda istanza di UserRepository (simulazione riavvio)...");
            UserRepository repoRead = new UserRepository(testFilePath);
            
            System.out.println("Caricamento dati da disco...");
            repoRead.loadFromDisk();

            User loadedUser = repoRead.getUser("test_giovanni");

            // FASE 3: Verifica dei dati ricaricati
            System.out.println("\n[3] Verifica integrità dati...");
            if (loadedUser == null) {
                System.err.println("[FALLITO] Utente non trovato dopo il caricamento da disco.");
                return;
            }

            boolean usernameOk = "test_giovanni".equals(loadedUser.getUsername());
            boolean hashOk = "hash_simulato_123".equals(loadedUser.getPasswordHash());
            boolean saltOk = "salt_simulato_456".equals(loadedUser.getSalt());

            System.out.println(" - Username corretto: " + usernameOk + " (" + loadedUser.getUsername() + ")");
            System.out.println(" - Hash corretto: " + hashOk);
            System.out.println(" - Salt corretto: " + saltOk);

            if (usernameOk && hashOk && saltOk) {
                System.out.println("\n[SUCCESSO] Il test di persistenza di UserRepository è SUPERATO!");
            } else {
                System.err.println("\n[FALLITO] I dati ricaricati differiscono da quelli originali.");
            }

        } catch (IOException e) {
            System.err.println("[ERRORE I/O] Eccezione durante il test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Pulizia finale del file di test
            if (testFile.exists()) {
                testFile.delete();
                System.out.println("\nFile di test temporaneo rimosso.");
            }
        }
    }
}