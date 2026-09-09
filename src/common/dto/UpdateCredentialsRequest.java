package common.dto;

/**
 * Oggetto di trasferimento dati (DTO) per la richiesta di modifica credenziali dell'utente.
 * <p>
 * Consente l'aggiornamento separato o combinato di username e password,
 * a fronte della presentazione valida delle credenziali correnti.
 */
public class UpdateCredentialsRequest {
    private String oldUsername;
    private String oldPsw;
    private String newUsername;
    private String newPsw;

    /** Costruttore di default per la deserializzazione JSON. */
    public UpdateCredentialsRequest(){}

    /**
     * Costruisce la richiesta specificando credenziali attuali e nuovi valori desiderati.
     *
     * @param oldUsername nome utente corrente
     * @param oldPsw password corrente
     * @param newUsername nuovo nome utente (può essere nullo se invariato)
     * @param newPsw nuova password (può essere nulla se invariata)
     */
    public UpdateCredentialsRequest(String oldUsername, String oldPsw, String newUsername, String newPsw){
        this.oldUsername = oldUsername;
        this.oldPsw = oldPsw;
        this.newUsername = newUsername;
        this.newPsw = newPsw;
    }

    /** Restituisce l'attuale nome utente associato all'account. */
    public String getOldUsername(){
        return oldUsername;
    }

    /** Restituisce l'attuale password dell'account. */
    public String getOldPsw(){
        return oldPsw;
    }

    /** Restituisce il nuovo username specificato. */
    public String getNewUsername(){
        return newUsername;
    }

    /** Restituisce la nuova password specificata. */
    public String getNewPsw(){
        return newPsw;
    }

    /**
     * Valida la richiesta verificando la presenza delle credenziali correnti
     * e che almeno uno dei due nuovi parametri sia specificato e valido
     * <p>
     * Query pura (nessun effetto collaterale).
     *
     * @return {@code true} se i vincoli formali sono soddisfatti, {@code false} altrimenti
     */
    public boolean isValid(){
        if(oldUsername == null || oldUsername.isBlank() || oldPsw == null || oldPsw.isBlank()){
            return false;
        }

        boolean hasValidNewUser = newUsername != null && !newUsername.isBlank();
        boolean hasValidNewPsw = newPsw != null && !newPsw.isBlank();

        if(newUsername != null && newUsername.isBlank()){
            return false;
        }
        if(newPsw != null && newPsw.isBlank()){
            return false;
        }
        return hasValidNewUser || hasValidNewPsw;

    }


}