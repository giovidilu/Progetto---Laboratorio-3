package common.dto;

/**
 * Oggetto di trasferimento dati (DTO) per le richieste di autenticazione e login utente.
 */
public class AuthRequest {
    private String username;
    private String psw;
    private Integer udpPort;

    /** Restituisce lo username richiesto. */
    public String getUsername(){ return username; }

    /** Restituisce la password in chiaro inviata. */
    public String getPsw(){ return psw; }

    /**
     * Verifica la validità dei parametri di autenticazione.
     * <p>
     * Query pura (nessun effetto collaterale).
     *
     * @return {@code true} se username e password sono valorizzati e non vuoti, {@code false} altrimenti
     */
    public boolean isValid(){
        return username != null && !username.isBlank() && psw != null && !psw.isBlank();
    }

    /** Restituisce la porta UDP locale per la ricezione notifiche. */
    public Integer getUdpPort() {
        return udpPort;
    }
}