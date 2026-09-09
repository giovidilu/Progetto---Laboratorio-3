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

    public LoginRequest(String username, String psw, Integer udpPort) {
        super(OperationType.LOGIN, username, psw);
        this.udpPort = udpPort;
    }

    public LoginRequest(String username, String psw) {
        this(username, psw, null);
    }

    public Integer getUdpPort() {
        return udpPort;
    }
}