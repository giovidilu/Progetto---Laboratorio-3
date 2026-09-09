package server.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import common.model.User;

/**
 * Repository persistente per la memorizzazione e gestione concorrente degli utenti.
 * <p>
 * Gestisce l'anagrafica, le credenziali e le statistiche storiche garantendo consistenza
 * in memoria e persistenza periodica su file JSON.
 * Thread-safe: tutte le operazioni sono sincronizzate sul monitor dell'istanza.
 */
public class UserRepository {
    private final ConcurrentHashMap<String, User> users;
    private final String filePath;
    private final Gson gson;

    /**
     * Inizializza il repository configurando il percorso di persistenza.
     *
     * @param filePath percorso del file JSON degli account utente
     */
    public UserRepository(String filePath){
        this.filePath = filePath;
        this.users = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();    
    }

    /**
     * Carica lo stato degli account registrati dal file JSON.
     * <p>
     * Se il file non esiste o è vuoto, il repository si predispone senza errori.
     * Metodo con effetto collaterale (mutazione della collezione interna), thread-safe e sincronizzato.
     *
     * @throws IOException se si verificano errori I/O non gestiti durante la lettura
     */
    public synchronized void loadFromDisk() throws IOException {
        Path path = Paths.get(this.filePath);
        if(!Files.exists(path) || Files.size(path) == 0){ 
            System.out.println("[USER-REPO] File " + filePath + " non trovato o vuoto: inizializzazione nuovo archivio utenti.");
            return;
        }

        try (FileReader reader = new FileReader(filePath, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType();
            ConcurrentHashMap<String, User> loadedUsers = gson.fromJson(reader, type);

            if (loadedUsers != null) {
                this.users.clear();
                this.users.putAll(loadedUsers);
            }
        } catch (com.google.gson.JsonSyntaxException e){
            System.err.println("[USER-REPO] Formato JSON non valido in " + filePath + ", inizializzazione mappa vuota: " + e.getMessage());
            this.users.clear();
        }
    }

    /**
     * Scrive su disco l'intero database utenti in formato JSON.
     * <p>
     * Metodo con effetto collaterale su file system, thread-safe e sincronizzato.
     *
     * @throws IOException se si verificano errori di scrittura su file
     */
    public synchronized void saveToDisk() throws IOException {
        Path path = Paths.get(filePath);
        
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(users, writer);
        }
    }

    /**
     * Registra un nuovo utente se il nome non è già presente a sistema.
     * <p>
     * Metodo con effetto collaterale, thread-safe e sincronizzato.
     *
     * @param user profilo utente da registrare
     * @return {@code true} se la registrazione ha successo, {@code false} se lo username è già registrato
     */
    public synchronized boolean addUser(User user) {
        User previous = users.putIfAbsent(user.getUsername(), user);
        return previous == null;
    }

    /**
     * Recupera il profilo di un utente tramite il relativo username.
     * <p>
     * Query pura, thread-safe e sincronizzata.
     *
     * @param username nome identificativo dell'utente
     * @return l'istanza di {@link User} cercata, o {@code null} se non presente
     */
    public synchronized User getUser(String username) {
        return users.get(username);
    }

    /**
     * Restituisce una copia della lista di tutti gli utenti registrati.
     * <p>
     * Query pura, thread-safe e sincronizzata.
     *
     * @return nuova lista contenente tutti gli oggetti {@link User} registrati
     */
    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    /**
     * Aggiorna in modo atomico username, password (hash e salt) o entrambi.
     * <p>
     * Verifica preventivamente l'esistenza di {@code oldUsername} e la disponibilità
     * del {@code newUsername}. Se il nome cambia, riallinea le chiavi della mappa interna.
     * Metodo con effetto collaterale, thread-safe e sincronizzato.
     *
     * @param oldUsername nome utente attuale
     * @param newUsername eventuale nuovo username (può essere nullo o coincidente)
     * @param newPasswordHash eventuale nuovo hash della password (o {@code null})
     * @param newSalt eventuale nuovo salt crittografico (o {@code null})
     * @return {@code true} se l'aggiornamento è andato a buon fine, {@code false} se l'utente non esiste
     *         o il nuovo username è già utilizzato da terzi
     */
    public synchronized boolean updateCredentials(String oldUsername, String newUsername, String newPasswordHash, String newSalt){
        User user = users.get(oldUsername);
        if(user == null ){
            return false;
        }

        if(newUsername != null && !newUsername.equals(oldUsername)){
            if (users.containsKey(newUsername)) {
                return false;
            }

            users.remove(oldUsername);
            user.setUsername(newUsername);

            if(newPasswordHash != null && newSalt != null){
                user.setPasswordHash(newPasswordHash);
                user.setSalt(newSalt);
            }

            users.put(newUsername, user);
            return true;
        }

        if(newPasswordHash != null && newSalt != null){
            user.setPasswordHash(newPasswordHash);
            user.setSalt(newSalt);
        }

        return true;
    }
}