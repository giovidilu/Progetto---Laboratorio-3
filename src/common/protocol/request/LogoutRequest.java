package common.protocol.request;

/**
 * Richiesta di disconnessione della sessione utente attiva.
 * <p>
 * Immutabile e thread-safe.
 */
public class LogoutRequest extends Request{

    /** Costruisce la richiesta impostando l'operazione di logout. */
    public LogoutRequest(){
        super("logout");
    }
}