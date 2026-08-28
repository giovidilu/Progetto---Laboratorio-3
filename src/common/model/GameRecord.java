package common.model;

public class GameRecord {
    private final int gameId;
    private int totalParticipants;
    private int participantsFinished;
    private int participantsWon;
    private double averageScore;

    public GameRecord(int gameId, int totalParticipants, int participantsFinished, int participantsWon, double averageScore) {
        this.gameId = gameId;
        this.totalParticipants = totalParticipants;
        this.participantsFinished = participantsFinished;
        this.participantsWon = participantsWon;
        this.averageScore = averageScore;
    }

    public int getGameId() {
        return gameId;
    }

    public int getTotalParticipants() {
        return totalParticipants;
    }

    public int getParticipantsFinished() {
        return participantsFinished;
    }

    public int getParticipantsWon() {
        return participantsWon;
    }

    public double getAverageScore() {
        return averageScore;
    }
}
