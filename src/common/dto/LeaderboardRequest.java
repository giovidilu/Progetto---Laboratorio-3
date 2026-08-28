package common.dto;

public class LeaderboardRequest {
    private String playerName;
    private Integer topPlayers;

    public LeaderboardRequest(){}

    public LeaderboardRequest(String playerName, Integer topPlayers){
        this.playerName = playerName;
        this.topPlayers = topPlayers;
    }

    public String getPlayerName(){
        return playerName;
    }

    public Integer getTopPlayer(){
        return topPlayers;
    }

    public boolean isValid(){
        if(playerName != null && playerName.isBlank()){
            return false;
        }
        if(topPlayers != null && topPlayers <= 0){
            return false;
        }

        return true;
    }
}
