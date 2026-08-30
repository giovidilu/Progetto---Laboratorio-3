package server.service;

import common.model.Game;
import common.model.GameOutcome;
import common.model.GameRecord;
import common.model.GameTemplate;
import common.model.MoveOutcome;
import common.model.PlayerGameState;
import common.model.ProposalResult;
import common.model.WordGroup;
import common.protocol.response.payload.GameInfoPayload;
import server.repository.GameRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private ScheduledExecutorService scheduler;
    private final Object lifecycleLock = new Object();

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

    /**
     * Valuta una proposta di 4 parole inviata da un utente per la partita attiva.
     * Metodo sincronizzato per garantire la consistenza dello stato di gioco e dei punteggi.
     *
     * @param username Nome dell'utente sottomittente.
     * @param words    Lista di 4 parole candidate a formare un gruppo tematico.
     * @return ProposalResult contenente l'esito della mossa, l'eventuale esito finale e lo stato aggiornato.
     */
    public synchronized ProposalResult submitProposal(String username, List<String> words) {
        // =========================================================================
        // FASE 1: Verifica di ammissibilità temporale e di stato (ALREADY_COMPLETED)
        // =========================================================================
        long now = System.currentTimeMillis();
        boolean isTimeExpired = (now >= this.activeGame.getEndTime());

        // Se il tempo è scaduto, blocca subito senza registrare utenti inattivi nella mappa
        if (isTimeExpired) {
            PlayerGameState existingState = this.activePlayerStates.get(username);
            GameOutcome outcome = (existingState != null) ? existingState.getOutcome() : null;
            return new ProposalResult(MoveOutcome.ALREADY_COMPLETED, outcome, null, existingState);
        }

        // Tempo valido: recupera lo stato esistente o registra il giocatore nella partita attiva
        PlayerGameState playerState = this.activePlayerStates.computeIfAbsent(
            username,
            u -> new PlayerGameState(u, this.currentGameId)
        );

        GameOutcome currentOutcome = playerState.getOutcome();
        if (currentOutcome != null) {
            return new ProposalResult(MoveOutcome.ALREADY_COMPLETED, currentOutcome, null, playerState);
        }

        // =========================================================================
        // FASE 2: Validazione sintattica della quadrupla (MALFORMED)
        // =========================================================================
        if (words == null || words.size() != 4) {
            return new ProposalResult(MoveOutcome.MALFORMED, null, null, playerState);
        }

        Set<String> proposalSet = new HashSet<>();
        for (String w : words) {
            if (w == null || w.trim().isEmpty()) {
                return new ProposalResult(MoveOutcome.MALFORMED, null, null, playerState);
            }
            proposalSet.add(w.trim().toUpperCase());
        }

        // Verifica unicità (esattamente 4 parole distinte)
        if (proposalSet.size() != 4) {
            return new ProposalResult(MoveOutcome.MALFORMED, null, null, playerState);
        }

        // Verifica appartenenza alle 16 parole della partita attiva
        Set<String> allGameWords = new HashSet<>();
        for (String w : this.activeGame.getShuffledWords()) {
            allGameWords.add(w.toUpperCase());
        }

        if (!allGameWords.containsAll(proposalSet)) {
            return new ProposalResult(MoveOutcome.MALFORMED, null, null, playerState);
        }

        // =========================================================================
        // FASE 3: Validazione semantica rispetto ai progressi del giocatore (MALFORMED)
        // =========================================================================
        Set<String> alreadyGuessedWords = new HashSet<>();
        for (WordGroup group : playerState.getCorrectGroups()) {
            if (group != null && group.getWords() != null) {
                for (String w : group.getWords()) {
                    alreadyGuessedWords.add(w.toUpperCase());
                }
            }
        }

        // Se la proposta include parole già indovinate in precedenza dallo stesso utente
        for (String w : proposalSet) {
            if (alreadyGuessedWords.contains(w)) {
                return new ProposalResult(MoveOutcome.MALFORMED, null, null, playerState);
            }
        }

        // =========================================================================
        // FASE 4: Valutazione del raggruppamento tematico (CORRECT o WRONG)
        // =========================================================================
        WordGroup matchedGroup = null;
        for (WordGroup group : this.activeGame.getGameTemplate().getGroups()) {
            if (group != null && group.getWords() != null) {
                Set<String> groupWords = new HashSet<>();
                for (String gw : group.getWords()) {
                    groupWords.add(gw.toUpperCase());
                }

                if (groupWords.equals(proposalSet)) {
                    matchedGroup = group;
                    break;
                }
            }
        }

        if (matchedGroup != null) {
            // Proposta Corretta
            playerState.addCorrectGroup(matchedGroup);
            GameOutcome newOutcome = playerState.getOutcome();
            return new ProposalResult(MoveOutcome.CORRECT, newOutcome, matchedGroup, playerState);
        } else {
            // Proposta Sbagliata
            playerState.incrementMistakes();
            GameOutcome newOutcome = playerState.getOutcome();
            return new ProposalResult(MoveOutcome.WRONG, newOutcome, null, playerState);
        }
    }

    /**
     * Consolida e archivia lo stato della partita corrente in GameRepository,
     * calcolando le statistiche aggregate ed avviando il round successivo.
     *
     * @return Il GameRecord consolidato della partita appena conclusa.
     */
    public synchronized GameRecord rotateGame() {
        // 1. Recupero dei gruppi corretti dal template del round concluso
        List<WordGroup> allGroups = this.activeGame.getGameTemplate().getGroups();

        // 2. Copia difensiva della mappa degli stati dei giocatori
        Map<String, PlayerGameState> playerStatesSnapshot = new HashMap<>(this.activePlayerStates);

        // 3. Calcolo delle statistiche aggregate
        int totalParticipants = playerStatesSnapshot.size();
        int participantsFinished = 0;
        int participantsWon = 0;
        int totalScoreSum = 0;

        for (PlayerGameState state : playerStatesSnapshot.values()) {
            GameOutcome outcome = state.getOutcome();
            if (outcome != null) {
                participantsFinished++;
                if (outcome == GameOutcome.WON) {
                    participantsWon++;
                }
            }
            totalScoreSum += state.getScore();
        }

        double averageScore = (totalParticipants > 0)
                ? ((double) totalScoreSum / totalParticipants)
                : 0.0;

        // 4. Creazione del GameRecord e inserimento nel repository
        GameRecord finishedRecord = new GameRecord(
            this.currentGameId,
            totalParticipants,
            participantsFinished,
            participantsWon,
            averageScore,
            allGroups,
            playerStatesSnapshot
        );

        this.gameRepository.addGameRecord(finishedRecord);

        // 5. Avvio della nuova partita e pulizia di activePlayerStates
        startNewActiveGame();

        return finishedRecord;
    }

    public synchronized int getCurrentGameId() {
        return this.currentGameId;
    }

    synchronized Game getActiveGame() {
        return this.activeGame;
    }

    
    public void start() {
        synchronized (lifecycleLock) {
            if (this.scheduler != null && !this.scheduler.isShutdown()) {
                return;
            }
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.scheduler.scheduleAtFixedRate(
                this::safeRotate,
                this.gameDurationMillis,
                this.gameDurationMillis,
                TimeUnit.MILLISECONDS
            );
        }
    }

    private void safeRotate() {
        try {
            rotateGame();
        } catch (Throwable t) {
            System.err.println("[GameManager] Errore imprevisto durante la rotazione periodica: " + t.getMessage());
        }
    }
    

    public void stop() {
        ScheduledExecutorService exec;
        synchronized (lifecycleLock) {
            if (this.scheduler == null || this.scheduler.isShutdown()) {
                return;
            }
            exec = this.scheduler;
        }

        exec.shutdown();
        try {
            if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                exec.shutdownNow();
            }
        } catch (InterruptedException e) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
}