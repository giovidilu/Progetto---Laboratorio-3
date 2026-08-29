package server.service;

import common.model.Game;
import common.model.GameRecord;
import common.model.GameTemplate;
import common.model.PlayerGameState;
import common.model.WordGroup;
import common.protocol.response.payload.GameInfoPayload;
import server.repository.GameRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestore centralizzato del ciclo di vita del gioco Connections.
 * Mantiene lo stato della partita attiva e fornisce l'accesso allo stato
 * delle partite attive e storiche.
 */
public class GameManager {

    private final Map<Integer, GameTemplate> templates;
    private final GameRepository gameRepository;
    private final long gameDurationMillis;
    private final ConcurrentHashMap<String, PlayerGameState> activePlayerStates;

    private int currentGameId;
    private Game activeGame;

    public GameManager(Map<Integer, GameTemplate> templates, GameRepository gameRepository, long gameDurationMillis) {
        if (templates == null || templates.isEmpty()) {
            throw new IllegalArgumentException("La mappa dei template non può essere nulla o vuota.");
        }
        this.templates = Collections.unmodifiableMap(templates);
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository non può essere null");
        this.gameDurationMillis = gameDurationMillis;
        this.activePlayerStates = new ConcurrentHashMap<>();

        startNewActiveGame();
    }

    public synchronized void startNewActiveGame() {
        this.currentGameId = this.gameRepository.generateGameId();

        int templateIndex = (this.currentGameId - 1) % this.templates.size();
        GameTemplate template = this.templates.get(templateIndex);

        if (template == null) {
            throw new IllegalStateException("Template non trovato per l'indice calcolato: " + templateIndex);
        }

        this.activeGame = new Game(template, this.gameDurationMillis);
        this.activePlayerStates.clear();
    }

    /**
     * Restituisce le informazioni sullo stato di una partita per uno specifico utente.
     * Rispetta il principio CQS: non crea voci in activePlayerStates in assenza di mosse.
     *
     * @param username Nome utente del richiedente.
     * @param gameId   ID della partita richiesta (null o pari a currentGameId indica la partita attiva).
     * @return GameInfoPayload valorizzato, oppure null se la partita richiesta non esiste.
     */
    public synchronized GameInfoPayload getGameInfoForPlayer(String username, Integer gameId) {
        if (gameId == null || gameId.equals(this.currentGameId)) {
            int timeRemaining = (int) Math.max(0, this.activeGame.getEndTime() - System.currentTimeMillis());

            PlayerGameState playerState = this.activePlayerStates.get(username);

            List<List<String>> correctGroups;
            List<String> remainingWords;
            int errors;
            int score;

            if (playerState == null) {
                correctGroups = Collections.emptyList();
                remainingWords = new ArrayList<>(this.activeGame.getShuffledWords());
                errors = 0;
                score = 0;
            } else {
                correctGroups = convertToWordLists(playerState.getCorrectGroups());
                remainingWords = calculateRemainingWords(playerState);
                errors = playerState.getMistakes();
                score = playerState.getScore();
            }

            return GameInfoPayload.OngoingGame(timeRemaining, correctGroups, remainingWords, errors, score);
        }

        GameRecord record = this.gameRepository.getGameRecord(gameId);
        if (record == null) {
            return null;
        }

        List<List<String>> finalAllocations = convertToWordLists(record.getAllGroups());

        PlayerGameState historicalState = (record.getPlayerStates() != null)
                ? record.getPlayerStates().get(username)
                : null;

        int numberCorrectGroups = (historicalState != null) ? historicalState.getCorrectGroups().size() : 0;
        int errors = (historicalState != null) ? historicalState.getMistakes() : 0;
        int score = (historicalState != null) ? historicalState.getScore() : 0;

        return GameInfoPayload.FinishedGame(finalAllocations, numberCorrectGroups, errors, score);
    }

    /**
     * Calcola le parole non ancora indovinate dal giocatore escludendo quelle
     * dei gruppi già scoperti.
     */
    private List<String> calculateRemainingWords(PlayerGameState playerState) {
        if (playerState == null || playerState.getCorrectGroups().isEmpty()) {
            return new ArrayList<>(this.activeGame.getShuffledWords());
        }

        Set<String> guessedWords = new HashSet<>();
        for (WordGroup group : playerState.getCorrectGroups()) {
            if (group != null && group.getWords() != null) {
                guessedWords.addAll(group.getWords());
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

    /**
     * Converte una lista di WordGroup in List<List<String>> per il payload
     */
    private List<List<String>> convertToWordLists(List<WordGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<String>> result = new ArrayList<>();
        for (WordGroup group : groups) {
            if (group != null && group.getWords() != null) {
                result.add(new ArrayList<>(group.getWords()));
            }
        }
        return result;
    }

    public synchronized int getCurrentGameId() {
        return this.currentGameId;
    }

    synchronized Game getActiveGame() {
        return this.activeGame;
    }
}