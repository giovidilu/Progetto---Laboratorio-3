package server.repository;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.model.GameRecord;

/**
 * Repository persistente per l'archiviazione e il recupero delle partite concluse.
 * <p>
 * Mantiene lo storico in una mappa concorrente in memoria centrale ed effettua la sincronizzazione
 * periodica su file system in formato JSON.
 * Thread-safe: garantisce atomicità mediante sincronizzazione esplicita sui metodi e l'uso di {@link AtomicInteger}.
 */
public class GameRepository {
    private final ConcurrentHashMap<Integer, GameRecord> games;
    private final String filePath;
    private final Gson gson;
    private final AtomicInteger idCounter;

    /**
     * Inizializza il repository associandolo al file di salvataggio specificato.
     *
     * @param filePath percorso del file JSON su disco
     */
    public GameRepository(String filePath){
        this.filePath = filePath;
        this.games = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.idCounter = new AtomicInteger();
    }
    
    /**
     * Carica lo storico delle partite dal file JSON allineando il contatore degli ID.
     * <p>
     * Se il file non esiste, l'archivio viene inizializzato vuoto senza sollevare eccezioni.
     * Metodo con effetto collaterale (mutazione della memoria interna), thread-safe e sincronizzato.
     *
     * @throws IOException se si verificano errori I/O non gestiti durante la lettura del file
     */
    public synchronized void loadFromDisk() throws IOException {
        Path path = Paths.get(this.filePath);
        if (!Files.exists(path)) { 
            System.out.println("[GAME-REPO] File " + filePath + " non trovato o vuoto: inizializzazione nuovo archivio partite.");
            return; 
        }

        try (FileReader reader = new FileReader(filePath, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<ConcurrentHashMap<Integer, GameRecord>>(){}.getType();
            ConcurrentHashMap<Integer, GameRecord> loadedGames = gson.fromJson(reader, type);
            if (loadedGames != null) {
                this.games.clear();
                this.games.putAll(loadedGames);

                int maxId = this.games.keySet().stream().max(Integer::compareTo).orElse(0);
                this.idCounter.set(maxId);
            }
        } catch(com.google.gson.JsonSyntaxException e){
            System.err.println("[GAME-REPO] Formato JSON non valido in " + filePath + ", inizializzazione mappa vuota: " + e.getMessage());
            this.games.clear();
        }
    }

    /**
     * Serializza l'intero archivio delle partite sul file JSON di destinazione.
     * <p>
     * Crea automaticamente le directory padri se mancanti.
     * Metodo con effetto collaterale su disco, thread-safe e sincronizzato.
     *
     * @throws IOException se si verificano errori durante la scrittura su file
     */
    public synchronized void saveToDisk() throws IOException {
        Path path = Paths.get(filePath);
        
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(games, writer);
        }
    }

    /**
     * Inserisce o aggiorna un record storico di partita completata.
     * <p>
     * Metodo con effetto collaterale, thread-safe e sincronizzato.
     *
     * @param record record della partita da registrare
     */
    public synchronized void addGameRecord(GameRecord record) {
        games.put(record.getGameId(), record);
    }
    
    /**
     * Recupera il record consolidato di una partita archiviata tramite il suo identificativo.
     * <p>
     * Query pura, thread-safe e sincronizzata.
     *
     * @param gameId identificativo univoco della partita da consultare
     * @return il {@link GameRecord} corrispondente, o {@code null} se non presente
     */
    public synchronized GameRecord getGameRecord(int gameId) {
        return games.get(gameId);
    }

    /**
     * Genera atomicamente un nuovo identificativo progressivo univoco per una partita.
     * <p>
     * Metodo con effetto collaterale sul contatore interno; thread-safe e lock-free tramite {@link AtomicInteger}.
     *
     * @return nuovo ID partita univoco incrementato
     */
    public int generateGameId(){
        return idCounter.incrementAndGet();
    }
}