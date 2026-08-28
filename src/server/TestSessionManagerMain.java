package server;

import server.service.SessionManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test in isolamento (Macro-3, Passo 4) per verificare la thread-safety di SessionManager
 * e la prevenzione del doppio login concorrente.
 */
public class TestSessionManagerMain {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== INIZIO TEST ISOLATO: SessionManager (Concorrenza e Doppio Login) ===");

        SessionManager sessionManager = new SessionManager();
        int numThreads = 10;
        String targetUser = "giovanni";

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numThreads);

        AtomicInteger successfulLogins = new AtomicInteger(0);
        AtomicInteger failedLogins = new AtomicInteger(0);

        // Creazione di 10 thread che tenteranno il login simultaneo con lo stesso username
        for (int i = 0; i < numThreads; i++) {
            executor.execute(() -> {
                try {
                    // Tutti i thread attendono qui per scattare nello stesso istante
                    startSignal.await();

                    boolean result = sessionManager.login(targetUser);
                    if (result) {
                        successfulLogins.incrementAndGet();
                    } else {
                        failedLogins.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        System.out.println("\n[1] Lancio di " + numThreads + " tentativi di login simultanei per l'utente '" + targetUser + "'...");
        startSignal.countDown(); // Sblocca tutti i thread contemporaneamente
        doneSignal.await();     // Attende il completamento di tutti i thread

        System.out.println(" - Login riusciti: " + successfulLogins.get());
        System.out.println(" - Login respinti: " + failedLogins.get());

        // Verifica 1: Uno e un solo thread deve aver ottenuto l'accesso
        if (successfulLogins.get() == 1 && failedLogins.get() == (numThreads - 1)) {
            System.out.println(" -> OK: Race condition evitata! Un solo login ha avuto successo.");
        } else {
            System.err.println(" -> FALLITO: Più thread sono riusciti ad autenticarsi contemporaneamente.");
            executor.shutdown();
            return;
        }

        // Verifica 2: Stato dell'utente
        boolean loggedIn = sessionManager.isLoggedIn(targetUser);
        System.out.println("\n[2] Utente '" + targetUser + "' risulta attivo: " + loggedIn);

        // Verifica 3: Logout e successivo re-login
        System.out.println("\n[3] Esecuzione logout per '" + targetUser + "'...");
        sessionManager.logout(targetUser);
        System.out.println("Utente risulta attivo dopo logout: " + sessionManager.isLoggedIn(targetUser));

        boolean reLogin = sessionManager.login(targetUser);
        System.out.println("Nuovo tentativo di login dopo il logout riuscito: " + reLogin);

        if (reLogin) {
            System.out.println("\n[SUCCESSO] Il test di SessionManager è SUPERATO!");
        } else {
            System.err.println("\n[FALLITO] Impossibile effettuare il login dopo il logout.");
        }

        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
    }
}