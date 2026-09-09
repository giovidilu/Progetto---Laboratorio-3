package common.protocol.request;

/**
 * Richiesta di recupero delle metriche e statistiche aggregate di una partita.
 * <p>
 * Permette di interrogare l'andamento in tempo reale della partita in corso o il riepilogo
 * di una partita passata (partecipanti, completamenti, vittorie, media punti).
 * Immutabile e thread-safe.
 */
public class RequestGameStatsRequest extends GameIdentifierRequest {

    /**
     * Crea la richiesta per le statistiche di una specifica partita archiviata.
     *
     * @param gameId identificativo univoco della partita
     */
    public RequestGameStatsRequest(int gameId) {
        super("requestGameStats", gameId);
    }

    /**
     * Crea la richiesta per le statistiche della partita attualmente attiva.
     */
    public RequestGameStatsRequest() {
        super("requestGameStats");
    }
}