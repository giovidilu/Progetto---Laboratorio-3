package common.protocol.request;

/**
 * Richiesta di disconnessione della sessione utente attiva.
 * <p>
 * Immutabile e thread-safe.
 */
public class LogoutRequest extends Request{

    public LogoutRequest(){
        super("logout");
    }
}