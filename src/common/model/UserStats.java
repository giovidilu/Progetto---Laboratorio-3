package common.model;

/**
 * Traccia e unisce le metriche storiche e le prestazioni complessive di un utente.
 * <p>
 * Mantiene il totale delle partite giocate, vinte, perse, le sequenze di vittorie (streak),
 * i puzzle risolti senza errori e l'istogramma dettagliato degli errori.
 * Thread-safe: tutte le operazioni di lettura e mutazione sono sincronizzate sul monitor
 * dell'istanza ({@code this}).
 */
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

    /**
     * Restituisce i dati dell'istogramma degli errori, istanziandoli se non presenti.
     * <p>
     * Thread-safe e sincronizzato.
     *
     * @return istanza di {@link MistakeHistogramData} associata all'utente
     */
    public synchronized MistakeHistogramData getMistakeHistogramData() {
        if (this.mistakeHistogram == null) {
            this.mistakeHistogram = new MistakeHistogramData();
        }
        return this.mistakeHistogram;
    }

    public synchronized MistakeHistogramData getMistakeHistogram() {
        return getMistakeHistogramData();
    }

    /**
     * Registra l'esito di una partita conclusa, aggiornando atomicamente punteggio cumulativo,
     * contatori di gioco, streak e istogramma degli errori.
     * <p>
     * Metodo con effetto collaterale (mutazione di stato), thread-safe e sincronizzato.
     *
     * @param outcome  esito della partita (se {@code null} viene trattato come {@link GameOutcome#DID_NOT_FINISH})
     * @param mistakes numero di errori commessi durante il match
     * @param score    punteggio netto conseguito (può essere positivo o negativo)
     */
    public synchronized void recordGameResult(GameOutcome outcome, int mistakes, int score) {
        this.totalScore += score;
        this.gamesPlayed++;

        if (outcome == null) {
            outcome = GameOutcome.DID_NOT_FINISH;
        }

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

    public synchronized int getTotalScore() {
        return totalScore;
    }

    public synchronized int getGamesPlayed() {
        return gamesPlayed;
    }

    public synchronized int getPuzzlesCompleted() {
        return gamesPlayed;
    }

    public synchronized int getGamesWon() {
        return gamesWon;
    }

    public synchronized int getPuzzlesWon() {
        return gamesWon;
    }

    public synchronized int getGamesLost() {
        return gamesLost;
    }

    public synchronized int getPuzzlesLost() {
        return gamesLost;
    }

    public synchronized int getCurrentStreak() {
        return currentStreak;
    }

    public synchronized int getMaxStreak() {
        return maxStreak;
    }

    public synchronized int getPerfectPuzzle() {
        return perfectPuzzle;
    }

    public synchronized int getPerfectPuzzles() {
        return perfectPuzzle;
    }

    /**
     * Calcola la percentuale di vittorie sulle partite giocate.
     * <p>
     * Query pura, thread-safe e sincronizzata.
     *
     * @return percentuale di vittorie (0.0 se non ci sono partite giocate)
     */
    public synchronized double getWinRate() {
        if (gamesPlayed == 0) {
            return 0.0;
        }
        return ((double) gamesWon / gamesPlayed) * 100.0;
    }

    public synchronized double getLostRate() {
        return getLossRate();
    }

    /**
     * Calcola la percentuale di sconfitte sulle partite giocate.
     * <p>
     * Query pura, thread-safe e sincronizzata.
     *
     * @return percentuale di sconfitte (0.0 se non ci sono partite giocate)
     */
    public synchronized double getLossRate() {
        if (gamesPlayed == 0) {
            return 0.0;
        }
        return ((double) gamesLost / gamesPlayed) * 100.0;
    }
}