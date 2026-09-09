package common.protocol.request;

/**
 * Classe base astratta per richieste di protocollo che richiedono credenziali (username e password).
 * <p>
 * Immutabile e thread-safe.
 */
public abstract class CredentialsRequest extends Request {
    private final String username;
    private final String psw;

    /**
     * Costruisce la richiesta associandovi l'operazione e le credenziali di accesso.
     *
     * @param operation nome del comando di protocollo
     * @param username nome utente del giocatore
     * @param psw password dell'utente
     */
    public CredentialsRequest(String operation, String username, String psw) {
        super(operation);
        this.username = username;
        this.psw = psw;
    }

    /** Restituisce il nome utente specificato. */
    public String getUsername() { return username; }

    /** Restituisce la password specificata. */
    public String getPsw() { return psw; }
}