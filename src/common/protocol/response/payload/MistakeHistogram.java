package common.protocol.response.payload;

/**
 * Rappresentazione serializzabile JSON dell'istogramma degli errori personali di un utente.
 * <p>
 * Modella la distribuzione delle partite concluse con successo in base al numero di errori commessi
 * (da 0 a 4), di quelle fallite al 4° errore e di quelle interrotte per tempo scaduto.
 * <p>
 * Classe immutabile e thread-safe.
 */
public class MistakeHistogram {
    private final int solvedWith0Mistakes;
    private final int solvedWith1Mistake;
    private final int solvedWith2Mistakes;
    private final int solvedWith3Mistakes;
    private final int solvedWith4Mistakes;
    private final int failed;
    private final int notFinished;

    /**
     * Costruisce l'oggetto istogramma con le occorrenze per ciascuna categoria di esito ed errore[cite: 112].
     *
     * @param solvedWith0Mistakes partite risolte con 0 errori
     * @param solvedWith1Mistake partite risolte con 1 errore
     * @param solvedWith2Mistakes partite risolte con 2 errori
     * @param solvedWith3Mistakes partite risolte con 3 errori
     * @param solvedWith4Mistakes partite risolte con 4 errori
     * @param failed partite fallite per esaurimento tentativi (4 errori)
     * @param notFinished partite non concluse prima dello scadere del tempo
     */
    public MistakeHistogram(int solvedWith0Mistakes, int solvedWith1Mistake, int solvedWith2Mistakes,
                             int solvedWith3Mistakes, int solvedWith4Mistakes, int failed, int notFinished) {
        this.solvedWith0Mistakes = solvedWith0Mistakes;
        this.solvedWith1Mistake = solvedWith1Mistake;
        this.solvedWith2Mistakes = solvedWith2Mistakes;
        this.solvedWith3Mistakes = solvedWith3Mistakes;
        this.solvedWith4Mistakes = solvedWith4Mistakes;
        this.failed = failed;
        this.notFinished = notFinished;
    }

    /** Restituisce le partite risolte con 0 errori. */
    public int getSolvedWith0Mistakes() { return solvedWith0Mistakes; }

    /** Restituisce le partite risolte con 1 errore. */
    public int getSolvedWith1Mistake() { return solvedWith1Mistake; }

    /** Restituisce le partite risolte con 2 errori. */
    public int getSolvedWith2Mistakes() { return solvedWith2Mistakes; }

    /** Restituisce le partite risolte con 3 errori. */
    public int getSolvedWith3Mistakes() { return solvedWith3Mistakes; }

    /** Restituisce le partite risolte con 4 errori. */
    public int getSolvedWith4Mistakes() { return solvedWith4Mistakes; }

    /** Restituisce le partite fallite per limite errori raggiunto. */
    public int getFailed() { return failed; }

    /** Restituisce le partite terminate per decorrenza del tempo limite. */
    public int getNotFinished() { return notFinished; }
}