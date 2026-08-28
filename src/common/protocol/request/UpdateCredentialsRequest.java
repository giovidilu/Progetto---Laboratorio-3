package common.protocol.request;

public class UpdateCredentialsRequest extends Request {
    
    private final String oldUsername;
    private final String oldPsw;
    private final String newUsername;
    private final String newPsw;

    /**
     * Costruttore privato. L'istanziazione dall'esterno deve avvenire 
     * obbligatoriamente tramite i metodi factory statici.
     */
    private UpdateCredentialsRequest(String oldUsername, String oldPsw, String newUsername, String newPsw) {
        super("updateCredentials");
        this.oldUsername = oldUsername;
        this.oldPsw = oldPsw;
        this.newUsername = newUsername;
        this.newPsw = newPsw;
    }

    /**
     * Per aggiornare sia Username che Psw
     */
    public static UpdateCredentialsRequest forBothUpdate(String oldUsername, String oldPsw, String newUsername, String newPsw) {
        return new UpdateCredentialsRequest(oldUsername, oldPsw, newUsername, newPsw);
    }

    /**
     * Per aggiornare solo Username
     * Il campo newPsw viene esplicitamente impostato a null.
     */
    public static UpdateCredentialsRequest forUsernameUpdate(String oldUsername, String oldPsw, String newUsername) {
        return new UpdateCredentialsRequest(oldUsername, oldPsw, newUsername, null);
    }

    /**
     * Per aggiornare solo Psw
     * Il campo newUsername viene esplicitamente impostato a null.
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
