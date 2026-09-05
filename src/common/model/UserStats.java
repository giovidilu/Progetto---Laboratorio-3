package common.model;

public class UserStats {
    private int totalScore;
    private int gamesPlayed;
    private int gamesWon;
    private int gamesLost;
    private int currentStreak;
    private int maxStreak;
    private int perfectPuzzle;
    private MistakeHistogramData mistakeHistogram;

    public UserStats() {
        this.totalScore = 0;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.gamesLost = 0;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.perfectPuzzle = 0;
        this.mistakeHistogram = new MistakeHistogramData();
    }

    public MistakeHistogramData getMistakeHistogramData() {
        if (this.mistakeHistogram == null) {
            this.mistakeHistogram = new MistakeHistogramData();
        }
        return this.mistakeHistogram;
    }

    public void recordGameResult(GameOutcome outcome, int mistakes, int score) {
        this.totalScore += score;
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

    public synchronized int getTotalScore(){ return totalScore; }
    public synchronized int getGamesPlayed() { return gamesPlayed; }
    public synchronized int getGamesWon() { return gamesWon; }
    public synchronized int getGamesLost() { return gamesLost; }
    public synchronized int getCurrentStreak() { return currentStreak; }
    public synchronized int getMaxStreak() { return maxStreak; }
    public synchronized int getPerfectPuzzles() { return perfectPuzzle; }

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