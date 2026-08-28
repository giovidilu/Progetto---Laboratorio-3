package common.dto;

public class GameQueryRequest {
    private Integer gameId;

    public GameQueryRequest(){}

    public GameQueryRequest(Integer gameId){
        this.gameId = gameId;
    }

    public Integer getGameId(){
        return gameId;
    }

    public boolean isValid(){
        return gameId == null || gameId > 0;
    }
}
