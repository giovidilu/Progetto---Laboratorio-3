package common.model;

public class UserStats {
    private int gamesPlayed = 0;
    private int gamesWon = 0;
    private int gamesLost = 0;
    private int currentStreak = 0;
    private int maxStreak = 0;
    private int perfectPuzzle = 0;
    private MistakeHistogramData mistakeHistogram;

    public UserStats() {}

    public MistakeHistogramData getMistakeHistogramData() {
        if (this.mistakeHistogram == null) {
            this.mistakeHistogram = new MistakeHistogramData();
        }
        return this.mistakeHistogram;
    }

    public void recordGameResult(GameOutcome outcome, int mistakes) {
        this.gamesPlayed++;

        switch (outcome) {
            case WON:
                this.gamesWon++;
                this.currentStreak++;
                if (this.currentStreak > this.maxStreak) {
                    this.maxStreak = this.currentStreak;
                }
                if (mistakes == 0) {
                    this.perfectPuzzle++;
                }
                getMistakeHistogramData().incrementSolvedWith(mistakes);
                break;
            case LOST_BY_MISTAKES:
                this.gamesLost++;
                this.currentStreak = 0;
                getMistakeHistogramData().incrementFailed();
                break;
            case DID_NOT_FINISH:
                this.currentStreak = 0;
                getMistakeHistogramData().incrementNotFinished();
                break;
            default:
                break;
        }
    }

    public int getGamesPlayed() { return gamesPlayed; }
    public int getGamesWon() { return gamesWon; }
    public int getGamesLost() { return gamesLost; }
    public int getCurrentStreak() { return currentStreak; }
    public int getMaxStreak() { return maxStreak; }
    public int getPerfectPuzzles() { return perfectPuzzle; }

    public double getWinRate() {
        if (gamesPlayed == 0) {
            return 0.0;
        }
        return ((double) gamesWon / gamesPlayed) * 100;
    }

    public double getLostRate() {
        if (gamesPlayed == 0) {
            return 0.0;
        }
        return ((double) gamesLost / gamesPlayed) * 100;
    }
}