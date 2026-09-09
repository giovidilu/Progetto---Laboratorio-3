package server.service;

import server.repository.GameRepository;
import server.repository.UserRepository;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servizio di background responsabile della persistenza periodica e del flush finale su disco.
 * <p>
 * Coordina la scrittura asincrona programmata delle collezioni gestite da {@link UserRepository} e
 * {@link GameRepository} in formato JSON.
 * Thread-safe: i metodi di avvio, arresto e salvataggio sono sincronizzati sul monitor dell'istanza.
 */
public class PersistenceManager {
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final long flushInterval;
    private final TimeUnit timeUnit;
    private ScheduledExecutorService scheduler;

    /**
     * Costruisce il componente associandolo ai repository e impostando l'intervallo temporale di flush.
     *
     * @param userRepository repository degli utenti da persistere
     * @param gameRepository repository delle partite da persistere
     * @param flushInterval cadenza di salvataggio
     * @param timeUnit unità di misura temporale associata all'intervallo
     */
    public PersistenceManager(UserRepository userRepository, GameRepository gameRepository, long flushInterval, TimeUnit timeUnit) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.flushInterval = flushInterval;
        this.timeUnit = timeUnit;
    }

    /**
     * Avvia il timer di background per il salvataggio automatico periodico.
     * <p>
     * Metodo thread-safe e idempotente.
     */
    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        this.scheduler.scheduleWithFixedDelay(() -> {
            try {
                saveAll();
            } catch (Exception e) {
                System.err.println("[PersistenceManager] Errore durante il salvataggio periodico: " + e.getMessage());
            }
        }, flushInterval, flushInterval, timeUnit);
    }

    /**
     * Forza il salvataggio sincrono su file system sia dell'archivio utenti che delle partite.
     * <p>
     * Metodo con effetto collaterale (scrittura I/O su disco), thread-safe e sincronizzato.
     *
     * @throws IOException se si verificano errori di scrittura nei file JSON
     */
    public synchronized void saveAll() throws IOException{
        userRepository.saveToDisk();
        gameRepository.saveToDisk();
    }

    /**
     * Arresta lo scheduler di background ed effettua il flush finale definitivo di tutti i dati su disco.
     * <p>
     * Metodo thread-safe concepito per essere invocato durante la fase di shutdown hook.
     */
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        try {
            saveAll();
            System.out.println("[PersistenceManager] Salvataggio finale completato con successo.");
        } catch (IOException e) {
            System.err.println("[PersistenceManager] Errore durante il salvataggio finale: " + e.getMessage());
        }
    }
}