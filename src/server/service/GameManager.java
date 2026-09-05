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
import common.protocol.response.payload.GameInfoPayload;
import common.protocol.response.payload.GameStatsPayload;
import common.protocol.response.payload.LeaderboardEntry;
import common.protocol.response.payload.LeaderboardPayload;
import common.protocol.response.payload.MistakeHistogram;
import common.protocol.response.payload.PlayerStatsPayload;
import server.repository.GameRepository;
import server.repository.UserRepository;

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
    private final UserRepository userRepository;
    private final long gameDurationMillis;
    private final ConcurrentHashMap<String, PlayerGameState> activePlayerStates;

    private ScheduledExecutorService scheduler;
    private final Object lifecycleLock = new Object();

    private int currentGameId;
    private Game activeGame;

    public GameManager(Map<Integer, GameTemplate> templates, GameRepository gameRepository, UserRepository userRepository, long gameDurationMillis) {
        if (templates == null || templates.isEmpty()) {
            throw new IllegalArgumentException("La mappa dei template non può essere nulla o vuota.");
        }
        this.templates = Collections.unmodifiableMap(templates);
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository non può essere null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository non può essere null");
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
     * Aggiorna in modo consistente e centralizzato le statistiche persistenti dell'utente.
     */
    private void updateUserStats(String username, GameOutcome outcome, int mistakes, int score) {
        User user = this.userRepository.getUser(username);
        if (user != null) {
            user.getStats().recordGameResult(outcome, mistakes, score);
        }
    }

    /**
     * Restituisce le informazioni sullo stato di una partita per uno specifico utente.
     * Rispetta il principio CQS: non crea voci in activePlayerStates in assenza di mosse.
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
     * Valuta una proposta di 4 parole inviata da un utente per la partita attiva.
     */
    public synchronized ProposalResult submitProposal(String username, List<String> words) {
        // FASE 1: Verifica di ammissibilità temporale e di stato (ALREADY_COMPLETED)
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

        // FASE 2: Validazione sintattica della quadrupla (MALFORMED)
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

        // FASE 3: Validazione semantica rispetto ai progressi del giocatore (MALFORMED)
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

        // FASE 4: Valutazione del raggruppamento tematico (CORRECT o WRONG)
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
            if (newOutcome != null) {
                updateUserStats(username, newOutcome, playerState.getMistakes(), playerState.getScore());
            }
            return new ProposalResult(MoveOutcome.CORRECT, newOutcome, matchedGroup, playerState);
        } else {
            // Proposta Sbagliata
            playerState.incrementMistakes();
            GameOutcome newOutcome = playerState.getOutcome();
            if (newOutcome != null) {
                updateUserStats(username, newOutcome, playerState.getMistakes(), playerState.getScore());
            }
            return new ProposalResult(MoveOutcome.WRONG, newOutcome, null, playerState);
        }
    }

    /**
     * Consolida e archivia lo stato della partita corrente in GameRepository,
     * calcolando le statistiche aggregate ed avviando il round successivo.
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

        // Aggiornamento delle statistiche persistenti esclusivamente per chi è rimasto non concluso
        for (PlayerGameState state : playerStatesSnapshot.values()) {
            if (state.getOutcome() == null) {
                updateUserStats(state.getUsername(), GameOutcome.DID_NOT_FINISH, state.getMistakes(), state.getScore());
            }
        }

        startNewActiveGame();

        return finishedRecord;
    }

    public synchronized GameStatsPayload getGameStats(Integer gameId) {
        if (gameId == null || gameId.equals(this.currentGameId)) {
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
        
        if (playerName != null) {
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