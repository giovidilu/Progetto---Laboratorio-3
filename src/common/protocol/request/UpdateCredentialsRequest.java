package common.protocol.request;

/**
 * Richiesta di protocollo per l'aggiornamento sicuro di username, password o entrambi.
 * <p>
 * Richiede la validazione preventiva delle credenziali correnti. L'istanziazione è vincolata
 * a factory method statici per garantire combinazioni di campi coerenti con il protocollo.
 * Immutabile e thread-safe.
 */
public class UpdateCredentialsRequest extends Request {
    
    private final String oldUsername;
    private final String oldPsw;
    private final String newUsername;
    private final String newPsw;

    private UpdateCredentialsRequest(String oldUsername, String oldPsw, String newUsername, String newPsw) {
        super("updateCredentials");
        this.oldUsername = oldUsername;
        this.oldPsw = oldPsw;
        this.newUsername = newUsername;
        this.newPsw = newPsw;
    }

    /**
     * Crea una richiesta per aggiornare contestualmente sia username che password.
     *
     * @param oldUsername username attualmente associato all'account
     * @param oldPsw password corrente necessaria per la validazione
     * @param newUsername nuovo username da impostare
     * @param newPsw nuova password da impostare
     * @return istanza configurata per il rinnovo completo delle credenziali
     */
    public static UpdateCredentialsRequest forBothUpdate(String oldUsername, String oldPsw, String newUsername, String newPsw) {
        return new UpdateCredentialsRequest(oldUsername, oldPsw, newUsername, newPsw);
    }

    /**
     * Crea una richiesta per aggiornare esclusivamente lo username.
     *
     * @param oldUsername username attualmente associato all'account
     * @param oldPsw password corrente necessaria per la validazione
     * @param newUsername nuovo username desiderato
     * @return istanza configurata per l'aggiornamento del solo username
     */
    public static UpdateCredentialsRequest forUsernameUpdate(String oldUsername, String oldPsw, String newUsername) {
        return new UpdateCredentialsRequest(oldUsername, oldPsw, newUsername, null);
    }

    /**
     * Crea una richiesta per aggiornare esclusivamente la password.
     *
     * @param oldUsername username attualmente associato all'account
     * @param oldPsw password corrente necessaria per la validazione
     * @param newPsw nuova password da impostare
     * @return istanza configurata per l'aggiornamento della sola password
     */
    public static UpdateCredentialsRequest forPasswordUpdate(String oldUsername, String oldPsw, String newPsw) {
        return new UpdateCredentialsRequest(oldUsername, oldPsw, null, newPsw);
    }

    public String getOldUsername() {
        return oldUsername;
    }

    public String getOldPsw() {
        return oldPsw;
    }

    public String getNewUsername() {
        return newUsername;
    }

    public String getNewPsw() {
        return newPsw;
    }
}