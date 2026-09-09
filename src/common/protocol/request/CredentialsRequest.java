package common.protocol.request;

/**
 * Classe base astratta per richieste di protocollo che richiedono credenziali (username e password).
 * <p>
 * Immutabile e thread-safe.
 */
public abstract class CredentialsRequest extends Request {
    private final String username;
    private final String psw;

    public CredentialsRequest(String operation, String username, String psw) {
        super(operation);
        this.username = username;
        this.psw = psw;
    }

    public String getUsername() { return username; }
    public String getPsw() { return psw; }
}