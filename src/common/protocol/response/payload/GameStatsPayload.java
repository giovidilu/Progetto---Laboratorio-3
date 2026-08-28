package common.protocol.response.payload;

import common.protocol.response.GameState;

public class GameStatsPayload {

    private final GameState state;

    // Campi specifici per partita in corso
    private final Integer timeRemaining;
    private final Integer playersStillPlaying;
    private final Integer playersFinished;
    private final Integer playersWon;

    // Campi specifici per partita conclusa
    private final Integer totalParticipants;
    private final Integer participantsFinished;
    private final Integer participantsWon;
    private final Double averageScore;

    private GameStatsPayload(GameState state, Integer timeRemaining, Integer playersStillPlaying,
                              Integer playersFinished, Integer playersWon, Integer totalParticipants,
                              Integer participantsFinished, Integer participantsWon, Double averageScore) {
        this.state = state;
        this.timeRemaining = timeRemaining;
        this.playersStillPlaying = playersStillPlaying;
        this.playersFinished = playersFinished;
        this.playersWon = playersWon;
        this.totalParticipants = totalParticipants;
        this.participantsFinished = participantsFinished;
        this.participantsWon = participantsWon;
        this.averageScore = averageScore;
    }

    public static GameStatsPayload ongoingGame(Integer timeRemaining, Integer playersStillPlaying,
                                                Integer playersFinished, Integer playersWon) {
        return new GameStatsPayload(GameState.ONGOING, timeRemaining, playersStillPlaying,
                playersFinished, playersWon, null, null, null, null);
    }

    public static GameStatsPayload finishedGame(Integer totalParticipants, Integer participantsFinished,
                                                 Integer participantsWon, Double averageScore) {
        return new GameStatsPayload(GameState.FINISHED, null, null, null, null,
                totalParticipants, participantsFinished, participantsWon, averageScore);
    }

    public GameState getState() { return state; }
    public Integer getTimeRemaining() { return timeRemaining; }
    public Integer getPlayersStillPlaying() { return playersStillPlaying; }
    public Integer getPlayersFinished() { return playersFinished; }
    public Integer getPlayersWon() { return playersWon; }
    public Integer getTotalParticipants() { return totalParticipants; }
    public Integer getParticipantsFinished() { return participantsFinished; }
    public Integer getParticipantsWon() { return participantsWon; }
    public Double getAverageScore() { return averageScore; }
}