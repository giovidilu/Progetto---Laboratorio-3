package common.protocol.request;

/**
 * Richiesta di consultazione dello stato o dell'esito di una partita.
 * <p>
 * Può puntare alla partita attiva oppure a una sessione archiviata mediante {@code gameId}.
 * Immutabile e thread-safe.
 */
public class RequestGameInfoRequest extends GameIdentifierRequest {

    /**
     * Crea la richiesta per una specifica partita storica.
     *
     * @param gameId identificativo univoco della partita da consultare
     */
    public RequestGameInfoRequest(int gameId) {
        super("requestGameInfo", gameId);
    }

    /**
     * Crea la richiesta implicitamente rivolta alla partita attualmente attiva sul server.
     */
    public RequestGameInfoRequest() {
        super("requestGameInfo");
    }
}