package common.model;

/**
 * Modello di aggregazione per l'istogramma degli errori e degli esiti di un utente.
 * <p>
 * Mantiene i contatori relativi alle partite risolte (da 0 a 4 errori), a quelle
 * fallite per superamento del limite errori e a quelle non concluse in tempo.
 * Non è thread-safe: l'accesso concorrente in scrittura deve essere sincronizzato esternamente.
 */
public class MistakeHistogramData {
    private int solvedWith0Mistakes = 0;
    private int solvedWith1Mistake = 0;
    private int solvedWith2Mistakes = 0;
    private int solvedWith3Mistakes = 0;
    private int solvedWith4Mistakes = 0;
    private int failed = 0;
    private int notFinished = 0;

    /**
     * Incrementa il contatore delle partite vinte con lo specifico numero di errori indicato.
     * <p>
     * Metodo con effetto collaterale (mutazione di stato).
     *
     * @param mistakes numero di errori commessi (atteso tra 0 e 3 per partite vinte)
     * @throws IllegalArgumentException se il parametro {@code mistakes} è negativo o non gestito
     */
    public void incrementSolvedWith(int mistakes){
        switch (mistakes) {
            case 0:
                solvedWith0Mistakes++;
                break;
            case 1:
                solvedWith1Mistake++;
                break;
            case 2:
                solvedWith2Mistakes++;
                break;
            case 3:
                solvedWith3Mistakes++;
                break;
            default:
                throw new IllegalArgumentException("Numero di errori non valido: " + mistakes);
        }
    }

    /** Incrementa il contatore delle partite perse per aver esaurito i tentativi consentiti. */
    public void incrementFailed() {
        failed++;
    }

    /** Incrementa il conteggio delle partite concluse per decorrenza del tempo limite. */
    public void incrementNotFinished() {
        notFinished++;
    }

    /** Restituisce il numero di partite risolte senza commettere errori. */
    public int getSolvedWith0Mistakes() { return solvedWith0Mistakes; }

    /** Restituisce il numero di partite risolte con 1 errore. */
    public int getSolvedWith1Mistake() { return solvedWith1Mistake; }

    /** Restituisce il numero di partite risolte con 2 errori. */
    public int getSolvedWith2Mistakes() { return solvedWith2Mistakes; }

    /** Restituisce il numero di partite risolte con 3 errori. */
    public int getSolvedWith3Mistakes() { return solvedWith3Mistakes; }

    /** Restituisce il numero di partite risolte con 4 errori. */
    public int getSolvedWith4Mistakes() { return solvedWith4Mistakes; }

    /** Restituisce il numero di partite perse per raggiungimento del limite errori. */
    public int getFailed() { return failed; }

    /** Restituisce il numero di partite non completate prima dello scadere del tempo. */
    public int getNotFinished() { return notFinished; }
}