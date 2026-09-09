package common.dto;

/**
 * Oggetto di trasferimento dati (DTO) per richieste mirate a una partita specifica o a quella attiva.
 */
public class GameQueryRequest {
    private Integer gameId;

    public GameQueryRequest(){}

    public GameQueryRequest(Integer gameId){
        this.gameId = gameId;
    }

    public Integer getGameId(){
        return gameId;
    }

    /**
     * Controlla la validità dell'identificativo partita specificato.
     * <p>
     * Query pura: accetta {@code null} (indicante la partita attiva) oppure identificativi numerici positivi.
     *
     * @return {@code true} se l'ID è nullo oppure strettamente positivo, {@code false} altrimenti
     */
    public boolean isValid(){
        return gameId == null || gameId > 0;
    }
}