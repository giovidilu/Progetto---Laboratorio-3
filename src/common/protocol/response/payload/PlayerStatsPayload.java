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

    /**
     * Costruisce il payload aggregato con le statistiche personali dell'utente.
     *
     * @param puzzlesCompleted totale delle partite giocate
     * @param winRate percentuale di vittorie
     * @param lossRate percentuale di sconfitte per errori
     * @param currentStreak sequenza corrente di vittorie consecutive
     * @param maxStreak massima sequenza storica di vittorie consecutive
     * @param perfectPuzzles totale di partite vinte con zero errori
     * @param mistakeHistogram distribuzione statistica degli errori
     */
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

    /** Restituisce il numero totale di puzzle completati o tentati. */
    public int getPuzzlesCompleted() { return puzzlesCompleted; }

    /** Restituisce la percentuale di puzzle vinti. */
    public double getWinRate() { return winRate; }

    /** Restituisce la percentuale di puzzle persi. */
    public double getLossRate() { return lossRate; }

    /** Restituisce la serie di vittorie consecutive attiva. */
    public int getCurrentStreak() { return currentStreak; }

    /** Restituisce la massima serie di vittorie consecutive raggiunta. */
    public int getMaxStreak() { return maxStreak; }

    /** Restituisce il conteggio di puzzle risolti con 0 errori. */
    public int getPerfectPuzzles() { return perfectPuzzles; }

    /** Restituisce l'istogramma dettagliato degli errori e degli esiti. */
    public MistakeHistogram getMistakeHistogram() { return mistakeHistogram; }
}