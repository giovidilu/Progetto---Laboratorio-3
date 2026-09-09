package common.protocol.request;

/**
 * Richiesta di consultazione delle statistiche personali storiche dell'utente autenticato.
 * <p>
 * Immutabile e thread-safe.
 */
public class RequestPlayerStatsRequest extends Request{

    /**
     * Costruisce la richiesta impostando l'operazione di protocollo corrispondente.
     */
    public RequestPlayerStatsRequest(){
        super("requestPlayerStats");
    }
}