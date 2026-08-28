package server.service;

import common.model.Game;
import common.model.GameOutcome;
import common.model.GameRecord;
import common.model.GameTemplate;
import common.model.PlayerGameState;
import common.model.WordGroup;
import common.protocol.response.payload.GameInfoPayload;
import server.repository.GameRepository;
import server.repository.GameTemplateLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestore centralizzato del ciclo di vita del gioco Connections.
 * Mantiene lo stato della partita globale attiva e processa le richieste
 * informative e di gioco dei singoli utenti.
 */
public class GameManager {

    private final GameTemplateLoader templateLoader;
    private final GameRepository gameRepository;
    private final long gameDurationMillis;

    // Struttura concorrente per associare a ciascun utente il proprio stato nella partita attiva
    private final ConcurrentHashMap<String, PlayerGameState> activePlayerStates;

    private int currentGameId;
    private Game activeGame;

    /**
     * Costruttore: inizializza le strutture dati e avvia la prima partita globale attiva.
     *
     * @param templateLoader     Caricatore dei modelli di gioco dal file JSON.
     * @param gameRepository     Repository per la memorizzazione e consultazione dello storico.
     * @param gameDurationMillis Durata prefissata di ogni partita in millisecondi.
     */
    public GameManager(GameTemplateLoader templateLoader, GameRepository gameRepository, long gameDurationMillis) {
        this.templateLoader = Objects.requireNonNull(templateLoader, "templateLoader non può essere null");
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository non può essere null");
        this.gameDurationMillis = gameDurationMillis;
        this.activePlayerStates = new ConcurrentHashMap<>();

        startNewActiveGame();
    }

    /**
     * Inizializza una nuova partita globale attiva prelevando lo schema corrispondente.
     */
    private void startNewActiveGame() {
        this.currentGameId = this.gameRepository.generateGameId();
        GameTemplate template = this.templateLoader.getTemplate(this.currentGameId);
        if (template == null) {
            throw new IllegalStateException("Impossibile caricare il template per il gameId: " + this.currentGameId);
        }

        this.activeGame = new Game(template, this.gameDurationMillis);
        this.activePlayerStates.clear();
    }

    /**
     * Restituisce le informazioni sullo stato di una partita per uno specifico utente.
     *
     * @param username Nome utente del richiedente.
     * @param gameId   ID della partita richiesta (se null o pari a currentGameId, indica la partita attiva).
     * @return GameInfoPayload con lo stato della partita, oppure null se la partita non esiste.
     */
    public GameInfoPayload getGameInfoForPlayer(String username, Integer gameId) {
        // Caso 1: Richiesta della Partita Attiva
        if (gameId == null || gameId.equals(this.currentGameId)) {
            long remainingTime = Math.max(0, this.activeGame.getEndTime() - System.currentTimeMillis());

            // Inizializzazione lazy dello stato per il giocatore
            PlayerGameState playerState = this.activePlayerStates.computeIfAbsent(
                username,
                u -> new PlayerGameState(u, this.currentGameId)
            );

            // Calcolo delle parole residue da raggruppare
            List<String> remainingWords = calculateRemainingWords(playerState);

            List<WordGroup> foundGroups = playerState.getGuessedGroups() != null
                    ? playerState.getGuessedGroups()
                    : Collections.emptyList();

            // Nella partita attiva le soluzioni complete (allGroups) rimangono segrete (null)
            return new GameInfoPayload(
                this.currentGameId,
                remainingTime,
                remainingWords,
                foundGroups,
                null,
                playerState.getMistakes(),
                playerState.getScore(),
                playerState.getOutcome()
            );
        }

        // Caso 2: Richiesta di una Partita Storica Conclusa
        GameRecord record = this.gameRepository.getGameRecord(gameId);
        if (record == null) {
            return null;
        }

        // Recupero dello stato registrato per quell'utente nello storico
        PlayerGameState historicalState = record.getPlayerStates() != null
                ? record.getPlayerStates().get(username)
                : null;

        List<WordGroup> foundGroups = (historicalState != null && historicalState.getGuessedGroups() != null)
                ? historicalState.getGuessedGroups()
                : Collections.emptyList();

        int mistakes = (historicalState != null) ? historicalState.getMistakes() : 0;
        int score = (historicalState != null) ? historicalState.getScore() : 0;
        GameOutcome outcome = (historicalState != null) ? historicalState.getOutcome() : null;

        return new GameInfoPayload(
            record.getGameId(),
            0L, // Il tempo residuo è 0 per partite concluse
            Collections.emptyList(), // Nessuna parola residua
            foundGroups,
            record.getAllGroups(), // Soluzioni complete visibili a partita conclusa
            mistakes,
            score,
            outcome
        );
    }

    /**
     * Calcola le parole non ancora indovinate dal giocatore sottraendo dall'elenco
     * iniziale le parole dei gruppi già correttamente individuati.
     */
    private List<String> calculateRemainingWords(PlayerGameState playerState) {
        Set<String> guessedWords = new HashSet<>();
        if (playerState.getGuessedGroups() != null) {
            for (WordGroup group : playerState.getGuessedGroups()) {
                if (group.getWords() != null) {
                    guessedWords.addAll(group.getWords());
                }
            }
        }

        List<String> remaining = new ArrayList<>();
        for (String word : this.activeGame.getShuffledWords()) {
            if (!guessedWords.contains(word)) {
                remaining.add(word);
            }
        }
        return remaining;
    }

    public int getCurrentGameId() {
        return currentGameId;
    }

    public Game getActiveGame() {
        return activeGame;
    }
}