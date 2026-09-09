package common.dto;

/**
 * Oggetto di trasferimento dati (DTO) per poter vedere la classifica.
 * Supporta la richiesta della classifica globale, dei primi K classificati o della posizione di un giocatore.
 */
public class LeaderboardRequest {
    private String playerName;
    private Integer topPlayers;

    /** Costruttore di default per deserializzazione. */
    public LeaderboardRequest(){}

    /** Costruisce la richiesta specificando giocatore e limite posizioni. */
    public LeaderboardRequest(String playerName, Integer topPlayers){
        this.playerName = playerName;
        this.topPlayers = topPlayers;
    }

    /** Restituisce il nome del giocatore di cui richiedere la posizione. */
    public String getPlayerName(){
        return playerName;
    }

    /** Restituisce il limite di giocatori per i quali estrarre il podio. */
    public Integer getTopPlayer(){
        return topPlayers;
    }

    /**
     * Verifica la correttezza formale dei criteri di filtraggio della classifica.
     * <p>
     * Query pura (nessun effetto collaterale).
     *
     * @return {@code true} se i parametri specificati rispettano i vincoli di consistenza, {@code false} altrimenti
     */
    public boolean isValid(){
        if(playerName != null && playerName.isBlank()){
            return false;
        }
        if(topPlayers != null && topPlayers < 0){
            return false;
        }

        return true;
    }
}