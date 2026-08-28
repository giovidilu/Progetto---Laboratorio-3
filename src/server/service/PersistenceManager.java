package server.service;

import server.repository.GameRepository;
import server.repository.UserRepository;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PersistenceManager {
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final long flushInterval;
    private final TimeUnit timeUnit;
    private ScheduledExecutorService scheduler;

    public PersistenceManager(UserRepository userRepository, GameRepository gameRepository, long flushInterval, TimeUnit timeUnit) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.flushInterval = flushInterval;
        this.timeUnit = timeUnit;
    }

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

    public synchronized void saveAll() throws IOException{
        userRepository.saveToDisk();
        gameRepository.saveToDisk();
    }

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