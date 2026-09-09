package common.protocol.request;

/**
 * Richiesta di registrazione di un nuovo account di gioco nel sistema.
 * <p>
 * Immutabile e thread-safe.
 */
public class RegisterRequest extends CredentialsRequest {

    /**
     * Costruisce la richiesta di registrazione per le credenziali specificate.
     *
     * @param username nome utente desiderato
     * @param psw password scelta
     */
    public RegisterRequest(String username, String psw) {
        super("register", username, psw);
    }
}