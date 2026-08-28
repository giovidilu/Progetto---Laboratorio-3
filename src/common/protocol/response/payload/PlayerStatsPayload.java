package common.protocol.response.payload;

public class PlayerStatsPayload {
    private final int puzzlesCompleted;
    private final double winRate;
    private final double lossRate;
    private final int currentStreak;
    private final int maxStreak;
    private final int perfectPuzzles;
    private final MistakeHistogram mistakeHistogram;

    public PlayerStatsPayload(int puzzlesCompleted, double winRate, double lossRate, int currentStreak,
                               int maxStreak, int perfectPuzzles, MistakeHistogram mistakeHistogram) {
        this.puzzlesCompleted = puzzlesCompleted;
        this.winRate = winRate;
        this.lossRate = lossRate;
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.perfectPuzzles = perfectPuzzles;
        this.mistakeHistogram = mistakeHistogram;
    }

    public int getPuzzlesCompleted() { return puzzlesCompleted; }
    public double getWinRate() { return winRate; }
    public double getLossRate() { return lossRate; }
    public int getCurrentStreak() { return currentStreak; }
    public int getMaxStreak() { return maxStreak; }
    public int getPerfectPuzzles() { return perfectPuzzles; }
    public MistakeHistogram getMistakeHistogram() { return mistakeHistogram; }
}