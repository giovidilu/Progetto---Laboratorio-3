package common.model;

/**
 * Traccia e unisce le metriche storiche e le prestazioni complessive di un utente.
 * <p>
 * Mantiene il totale delle partite giocate, vinte, perse, le sequenze di vittorie (streak),
 * i puzzle risolti senza errori e l'istogramma dettagliato degli errori.
 * Thread-safe: tutte le operazioni di lettura e mutazione sono sincronizzate sul monitor
 * dell'istanza ({@code this})[cite: 87].
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

    /**
     * Costruisce una nuova istanza di statistiche azzerando tutti i contatori iniziali.
     */
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

    /**
     * Alias per {@link #getMistakeHistogramData()}; restituisce i dati dell'istogramma errori.
     *
     * @return l'oggetto {@link MistakeHistogramData}
     */
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

    /** Restituisce il punteggio cumulativo totale dell'utente. */
    public synchronized int getTotalScore() {
        return totalScore;
    }

    /** Restituisce il numero totale di partite giocate. */
    public synchronized int getGamesPlayed() {
        return gamesPlayed;
    }

    /** Restituisce il numero totale di partite completate. */
    public synchronized int getPuzzlesCompleted() {
        return gamesPlayed;
    }

    /** Restituisce il numero di partite vinte dall'utente. */
    public synchronized int getGamesWon() {
        return gamesWon;
    }

    /** Restituisce il numero di puzzle vinti dall'utente. */
    public synchronized int getPuzzlesWon() {
        return gamesWon;
    }

    /** Restituisce il numero di partite perse dall'utente. */
    public synchronized int getGamesLost() {
        return gamesLost;
    }

    /** Restituisce il numero di puzzle persi dall'utente. */
    public synchronized int getPuzzlesLost() {
        return gamesLost;
    }

    /** Restituisce la serie attuale di vittorie consecutive. */
    public synchronized int getCurrentStreak() {
        return currentStreak;
    }

    /** Restituisce la massima serie di vittorie consecutive mai registrata. */
    public synchronized int getMaxStreak() {
        return maxStreak;
    }

    /** Restituisce il conteggio delle partite risolte senza commettere alcun errore. */
    public synchronized int getPerfectPuzzle() {
        return perfectPuzzle;
    }

    /** Restituisce il conteggio dei puzzle perfetti (0 errori). */
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

    /** Calcola la percentuale di sconfitte sulle partite giocate. */
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