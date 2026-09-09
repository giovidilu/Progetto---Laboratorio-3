package server.service;

import common.model.Game;
import common.model.GameOutcome;
import common.model.GameRecord;
import common.model.GameTemplate;
import common.model.MistakeHistogramData;
import common.model.MoveOutcome;
import common.model.PlayerGameState;
import common.model.ProposalResult;
import common.model.User;
import common.model.UserStats;
import common.model.WordGroup;
import common.protocol.response.payload.GameFinishedNotificationPayload;
import common.protocol.response.payload.GameInfoPayload;
import common.protocol.response.payload.GameStatsPayload;
import common.protocol.response.payload.LeaderboardEntry;
import common.protocol.response.payload.LeaderboardPayload;
import common.protocol.response.payload.MistakeHistogram;
import common.protocol.response.payload.PlayerStatsPayload;
import server.repository.GameRepository;
import server.repository.UserRepository;

import java.net.InetSocketAddress;
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

import com.google.gson.Gson;

/**
 * Gestore centralizzato del ciclo di vita del gioco Connections.
 * <p>
 * Coordina lo svolgimento dell'unica partita attiva a livello globale, valida e applica
 * le proposte inviate dai giocatori, aggiorna le statistiche individuali e storiche degli utenti,
 * gestisce il timer di round e orchestra l'inoltro delle notifiche UDP asincrone alla conclusione.
 * <p>
 * Thread-safety garantita tramite sincronizzazione intrinseca sui metodi di business logic e collezioni concorrenti.
 */
public class GameManager {

    private final Map<Integer, GameTemplate> templates;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final long gameDurationMillis;
    private final ConcurrentHashMap<String, PlayerGameState> activePlayerStates;
    private final SessionManager sessionManager;
    private final UdpNotifier udpNotifier;

    private ScheduledExecutorService scheduler;
    private final Object lifecycleLock = new Object();

    private int currentGameId;
    private Game activeGame;

    /**
     * Costruisce il gestore inizializzando le dipendenze e predisponendo la prima partita attiva.
     *
     * @param templates mappa dei template di puzzle disponibili indicizzata per id
     * @param gameRepository repository per l'archiviazione dello storico partite
     * @param userRepository repository per l'aggiornamento persistente dei profili utente
     * @param sessionManager gestore delle sessioni e degli endpoint UDP attivi
     * @param udpNotifier componente per la trasmissione di notifiche asincrone su datagrammi
     * @param gameDurationMillis durata prefissata di ciascuna sessione globale in millisecondi
     */
    public GameManager(Map<Integer, GameTemplate> templates, 
                       GameRepository gameRepository, 
                       UserRepository userRepository, 
                       SessionManager sessionManager, 
                       UdpNotifier udpNotifier, 
                       long gameDurationMillis) {
        if (templates == null || templates.isEmpty()) {
            throw new IllegalArgumentException("La mappa dei template non può essere nulla o vuota.");
        }
        this.templates = Collections.unmodifiableMap(templates);
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository non può essere null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository non può essere null");
        this.sessionManager = sessionManager;
        this.udpNotifier = udpNotifier;
        this.gameDurationMillis = gameDurationMillis;
        this.activePlayerStates = new ConcurrentHashMap<>();

        startNewActiveGame();
    }

    /**
     * Costruttore semplificato per ambienti di test privi di stack di notifica asincrona.
     */
    public GameManager(Map<Integer, GameTemplate> templates, 
                       GameRepository gameRepository, 
                       UserRepository userRepository, 
                       long gameDurationMillis) {
        this(templates, gameRepository, userRepository, null, null, gameDurationMillis);
    }

    /**
     * Inizializza un nuovo turno globale incrementando il progressivo partita, selezionando ciclicamente
     * il template corrispondente e azzerando gli stati provvisori dei giocatori correnti.
     * <p>
     * Metodo con effetto collaterale, sincronizzato e thread-safe.
     */
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

    private void updateUserStats(String username, GameOutcome outcome, int mistakes, int score) {
        User user = this.userRepository.getUser(username);
        if (user != null) {
            user.getStats().recordGameResult(outcome, mistakes, score);
        }
    }

    /**
     * Restituisce lo stato di gioco per {@code username}, sulla partita corrente
     * (se {@code gameId} è {@code null}, {@code 0}, o coincide con quella attiva)
     * oppure su una partita storica archiviata.
     * <p>
     * Se l'utente non ha ancora effettuato mosse nella partita richiesta (attiva o
     * storica), i campi di progresso (errori, punteggio, gruppi indovinati) sono
     * riportati a zero/vuoti, senza che ciò implichi la creazione di uno stato
     * persistente per lui.
     * <p>
     * Query pura (nessun effetto collaterale) e thread-safe.
     *
     * @param username utente per cui recuperare lo stato
     * @param gameId id della partita, o {@code null}/{@code 0} per quella corrente
     * @return il payload della partita corrente o storica; {@code null} se
     *         {@code gameId} non corrisponde ad alcuna partita storica esistente
     */
    public synchronized GameInfoPayload getGameInfoForPlayer(String username, Integer gameId) {
        if (gameId == null || gameId == 0 || gameId.equals(this.currentGameId)) {
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
     * Valida ed esegue una proposta di 4 parole per la partita corrente da parte dell'utente.
     * <p>
     * Se la sessione è scaduta o l'utente ha già vinto/perso, la mossa viene respinta senza mutare lo stato.
     * Se le parole sono non valide, duplicate o già indovinate, viene restituito esito {@link MoveOutcome#MALFORMED}
     * senza addebitare penalità. In caso di esito corretto o errato, aggiorna punteggio ed errori del giocatore,
     * verificando le condizioni di vittoria (al 3° gruppo) o sconfitta (al 4° errore) e aggiornando le statistiche utente.
     * <p>
     * Metodo con effetti collaterali marcati, sincronizzato e thread-safe.
     *
     * @param username nome identificativo del giocatore
     * @param words quadrupla di vocaboli proposti
     * @return esito complessivo {@link ProposalResult} contenente risultato della mossa, eventuale esito finale e stato aggiornato
     */
    public synchronized ProposalResult submitProposal(String username, List<String> words) {
        long now = System.currentTimeMillis();
        boolean isTimeExpired = (now >= this.activeGame.getEndTime());

        if (isTimeExpired) {
            PlayerGameState existingState = this.activePlayerStates.get(username);
            GameOutcome outcome = (existingState != null) ? existingState.getOutcome() : null;
            return new ProposalResult(MoveOutcome.ALREADY_COMPLETED, outcome, null, existingState);
        }

        PlayerGameState playerState = this.activePlayerStates.computeIfAbsent(
            username,
            u -> new PlayerGameState(u, this.currentGameId)
        );

        GameOutcome currentOutcome = playerState.getOutcome();
        if (currentOutcome != null) {
            return new ProposalResult(MoveOutcome.ALREADY_COMPLETED, currentOutcome, null, playerState);
        }

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

        if (proposalSet.size() != 4) {
            return new ProposalResult(MoveOutcome.MALFORMED, null, null, playerState);
        }

        Set<String> allGameWords = new HashSet<>();
        for (String w : this.activeGame.getShuffledWords()) {
            allGameWords.add(w.toUpperCase());
        }

        if (!allGameWords.containsAll(proposalSet)) {
            return new ProposalResult(MoveOutcome.MALFORMED, null, null, playerState);
        }

        Set<String> alreadyGuessedWords = new HashSet<>();
        for (WordGroup group : playerState.getCorrectGroups()) {
            if (group != null && group.getWords() != null) {
                for (String w : group.getWords()) {
                    alreadyGuessedWords.add(w.toUpperCase());
                }
            }
        }

        for (String w : proposalSet) {
            if (alreadyGuessedWords.contains(w)) {
                return new ProposalResult(MoveOutcome.MALFORMED, null, null, playerState);
            }
        }

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
            playerState.addCorrectGroup(matchedGroup);
            
            if (playerState.getOutcome() == GameOutcome.WON && playerState.getCorrectGroups().size() == 3) {
                for (WordGroup templateGroup : this.activeGame.getGameTemplate().getGroups()) {
                    boolean alreadyPresent = false;
                    for (WordGroup guessed : playerState.getCorrectGroups()) {
                        if (guessed.getWords().equals(templateGroup.getWords())) {
                            alreadyPresent = true;
                            break;
                        }
                    }
                    if (!alreadyPresent) {
                        playerState.addCorrectGroup(templateGroup);
                        break;
                    }
                }
            }

            GameOutcome newOutcome = playerState.getOutcome();
            if (newOutcome != null) {
                updateUserStats(username, newOutcome, playerState.getMistakes(), playerState.getScore());
            }
            return new ProposalResult(MoveOutcome.CORRECT, newOutcome, matchedGroup, playerState);
        } else {
            playerState.incrementMistakes();
            GameOutcome newOutcome = playerState.getOutcome();
            if (newOutcome != null) {
                updateUserStats(username, newOutcome, playerState.getMistakes(), playerState.getScore());
            }
            return new ProposalResult(MoveOutcome.WRONG, newOutcome, null, playerState);
        }
    }

    /**
     * Conclude la partita attiva consolidando le statistiche aggregate nel {@link GameRepository},
     * registrando l'esito DNF per i partecipanti incompleti, avviando un nuovo round e inviando
     * le notifiche asincrone UDP con le soluzioni integrali ai client autenticati.
     * <p>
     * Metodo con evidenti effetti collaterali di ciclo vita, sincronizzato e thread-safe.
     *
     * @return il {@link GameRecord} storicizzato relativo alla partita appena terminata
     */
    public synchronized GameRecord rotateGame() {
        List<WordGroup> allGroups = this.activeGame.getGameTemplate().getGroups();
        Map<String, PlayerGameState> playerStatesSnapshot = new HashMap<>(this.activePlayerStates);

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

        int finishedGameId = this.currentGameId;
        GameRecord finishedRecord = new GameRecord(
            finishedGameId,
            totalParticipants,
            participantsFinished,
            participantsWon,
            averageScore,
            allGroups,
            playerStatesSnapshot
        );
        this.gameRepository.addGameRecord(finishedRecord);

        for (PlayerGameState state : playerStatesSnapshot.values()) {
            if (state.getOutcome() == null) {
                updateUserStats(state.getUsername(), GameOutcome.DID_NOT_FINISH, state.getMistakes(), state.getScore());
            }
        }

        startNewActiveGame();

        if (this.udpNotifier != null && this.sessionManager != null) {
            GameStatsPayload finishedStats = GameStatsPayload.finishedGame(
                finishedRecord.getTotalParticipants(),
                finishedRecord.getParticipantsFinished(),
                finishedRecord.getParticipantsWon(),
                finishedRecord.getAverageScore()
            );

            Gson gson = new Gson();
            Map<String, InetSocketAddress> activeEndpoints = this.sessionManager.getActiveUdpEndpoints();

            for (Map.Entry<String, InetSocketAddress> entry : activeEndpoints.entrySet()) {
                String username = entry.getKey();
                InetSocketAddress endpoint = entry.getValue();

                try {
                    GameInfoPayload playerInfo = getGameInfoForPlayer(username, finishedGameId);
                    GameFinishedNotificationPayload notifPayload = new GameFinishedNotificationPayload(
                        finishedGameId,
                        playerInfo,
                        finishedStats
                    );
                    this.udpNotifier.sendNotification(endpoint, gson.toJson(notifPayload));
                } catch (Exception e) {
                    System.err.println("[GameManager] Invio notifica UDP fallito per " + username + ": " + e.getMessage());
                }
            }
        }

        return finishedRecord;
    }

    /**
     * Restituisce le statistiche aggregate di una partita attiva o archiviata.
     * <p>
     * Query pura (nessun effetto collaterale) e thread-safe.
     *
     * @param gameId identificativo della partita, oppure {@code null}/{@code 0} per quella in corso
     * @return payload {@link GameStatsPayload} con le metriche calcolate; {@code null} se la partita richiesta non esiste
     */
    public synchronized GameStatsPayload getGameStats(Integer gameId) {
        if (gameId == null || gameId == 0 || gameId.equals(this.currentGameId)) {
            int timeRemaining = (int) Math.max(0, this.activeGame.getEndTime() - System.currentTimeMillis());
            
            int playersFinished = 0;
            int playersWon = 0;

            for (PlayerGameState state : this.activePlayerStates.values()) {
                GameOutcome outcome = state.getOutcome();
                if (outcome != null) {
                    playersFinished++;
                    if (outcome == GameOutcome.WON) {
                        playersWon++;
                    }
                }
            }

            int playersPlaying = this.activePlayerStates.size() - playersFinished;

            return GameStatsPayload.ongoingGame(timeRemaining, playersPlaying, playersFinished, playersWon);
        }

        GameRecord record = this.gameRepository.getGameRecord(gameId);
        if (record == null) {
            return null;
        }

        return GameStatsPayload.finishedGame(
            record.getTotalParticipants(),
            record.getParticipantsFinished(),
            record.getParticipantsWon(),
            record.getAverageScore()
        );
    }

    /**
     * Calcola la classifica globale ordinata per punteggio decrescente e username alfabetico.
     * <p>
     * Supporta il filtraggio per podio/top-K o per singolo giocatore.
     * Query pura (nessun effetto collaterale) e thread-safe.
     *
     * @param topPlayers numero massimo di posizioni da restituire (o {@code null} per tutti)
     * @param playerName eventuale username per cui estrarre solo la posizione individuale
     * @return il payload {@link LeaderboardPayload}; {@code null} se {@code playerName} non esiste a sistema
     */
    public synchronized LeaderboardPayload getLeaderboard(Integer topPlayers, String playerName) {
        List<User> allUsers = this.userRepository.getAllUsers();

        allUsers.sort((u1, u2) -> {
            int scoreCompare = Integer.compare(u2.getStats().getTotalScore(), u1.getStats().getTotalScore());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return u1.getUsername().compareTo(u2.getUsername());
        });

        List<LeaderboardEntry> fullLeaderboard = new ArrayList<>();
        LeaderboardEntry targetUserEntry = null;
        for (int i = 0; i < allUsers.size(); i++) {
            User user = allUsers.get(i);
            int rank = i + 1;
            LeaderboardEntry entry = new LeaderboardEntry(rank, user.getUsername(), user.getStats().getTotalScore());
            fullLeaderboard.add(entry);
            if (playerName != null && playerName.equals(user.getUsername())) {
                targetUserEntry = entry;
            }
        }
        
        if (playerName != null && !playerName.trim().isEmpty()) {
            if (targetUserEntry == null) {
                return null;
            }
            return new LeaderboardPayload(Collections.singletonList(targetUserEntry));
        }

        List<LeaderboardEntry> finalLeaderboard;
        if (topPlayers != null && topPlayers > 0 && topPlayers < fullLeaderboard.size()) {
            finalLeaderboard = new ArrayList<>(fullLeaderboard.subList(0, topPlayers));
        } else {
            finalLeaderboard = fullLeaderboard;
        }

        return new LeaderboardPayload(finalLeaderboard);
    }

    /**
     * Restituisce il profilo statistico storico dell'utente indicato (partite, percentuali, streak, istogramma).
     * <p>
     * Query pura (nessun effetto collaterale) e thread-safe.
     *
     * @param username utente di cui estrarre le metriche
     * @return payload {@link PlayerStatsPayload} popolato; {@code null} se l'utente non è registrato
     */
    public synchronized PlayerStatsPayload getPlayerStats(String username) {
        User user = userRepository.getUser(username);
        if (user == null) {
            return null;
        }

        UserStats stats = user.getStats();
        MistakeHistogramData domainHist = stats.getMistakeHistogramData();

        MistakeHistogram payloadHist = new MistakeHistogram(
            domainHist.getSolvedWith0Mistakes(),
            domainHist.getSolvedWith1Mistake(),
            domainHist.getSolvedWith2Mistakes(),
            domainHist.getSolvedWith3Mistakes(),
            domainHist.getSolvedWith4Mistakes(),
            domainHist.getFailed(),
            domainHist.getNotFinished()
        );

        return new PlayerStatsPayload(
            stats.getPuzzlesCompleted(),
            stats.getWinRate(),
            stats.getLossRate(),
            stats.getCurrentStreak(),
            stats.getMaxStreak(),
            stats.getPerfectPuzzles(),
            payloadHist
        );
    }

    public synchronized int getCurrentGameId() {
        return this.currentGameId;
    }

    synchronized Game getActiveGame() {
        return this.activeGame;
    }

    /**
     * Avvia l'esecutore periodico che pianifica la rotazione automatica del gioco allo scadere del round.
     * <p>
     * Thread-safe e idempotente.
     */
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

    /**
     * Arresta in modo ordinato il timer periodico di avanzamento turni.
     * <p>
     * Thread-safe.
     */
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