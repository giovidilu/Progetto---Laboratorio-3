package common.protocol.request;

import common.protocol.OperationType;

/**
 * Richiesta di autenticazione utente al servizio.
 * <p>
 * Include le credenziali di accesso e la porta effimera locale su cui il client
 * resta in ascolto per ricevere le notifiche UDP asincrone di fine partita.
 * Immutabile e thread-safe.
 */
public class LoginRequest extends CredentialsRequest {
    private final Integer udpPort;

    /**
     * Crea la richiesta di login specificando credenziali ed eventuale porta UDP per notifiche.
     *
     * @param username nome utente
     * @param psw password
     * @param udpPort porta UDP locale di ricezione notifiche (può essere nulla)
     */
    public LoginRequest(String username, String psw, Integer udpPort) {
        super(OperationType.LOGIN, username, psw);
        this.udpPort = udpPort;
    }

    /**
     * Crea la richiesta di login priva di porta UDP di notifica.
     *
     * @param username nome utente
     * @param psw password
     */
    public LoginRequest(String username, String psw) {
        this(username, psw, null);
    }

    /** Restituisce la porta UDP configurata per il client. */
    public Integer getUdpPort() {
        return udpPort;
    }
}