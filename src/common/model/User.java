package common.model;

/**
 * Rappresenta un account utente registrato nel sistema con credenziali e profilo statistico.
 * <p>
 * Memorizza hash crittografico e relativo salt per la verifica sicura della password,
 * assieme all'oggetto {@link UserStats} per la tracciatura delle metriche di gioco.
 * Non è thread-safe: l'accesso e l'aggiornamento devono essere coordinati tramite repository o servizi sincronizzati.
 */
public class User {
    private String username;
    private String passwordHash;
    private String salt;
    private UserStats stats;

    public User(){
        this.stats = new UserStats();
    }

    public User(String username, String passwordHash, String salt){
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.stats = new UserStats();
    }

    public String getUsername(){
        return username;
    }
    public void setUsername(String username){
        this.username = username;
    }

    public String getPasswordHash(){
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public String getSalt(){
        return salt;
    }
    public void setSalt(String salt){
        this.salt = salt;
    }

    /**
     * Restituisce le statistiche dell'utente garantendone l'inizializzazione difensiva se nulle.
     *
     * @return istanza di {@link UserStats} associata al profilo
     */
    public UserStats getStats(){
        if(this.stats ==  null){
            this.stats = new UserStats();
        } 
        return this.stats;
    }

    public void setStats(UserStats stats){
        this.stats = stats;
    }

}