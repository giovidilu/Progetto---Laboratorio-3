package common.protocol.response.payload;

/**
 * Payload di risposta per l'operazione {@code requestPlayerStats}, modellato secondo
 * le specifiche statistiche personali in stile New York Times Connections.
 * <p>
 * Riassume partite completate, percentuali di vittoria/sconfitta, serie di vittorie consecutive
 * (corrente e massima), partite perfette a zero errori e istogramma dettagliato.
 * <p>
 * Classe immutabile e thread-safe.
 */
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