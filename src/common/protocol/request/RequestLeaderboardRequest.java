package common.protocol.request;

public class RequestLeaderboardRequest extends Request {
    private final String playerName;
    private final Integer topPlayers;

    private RequestLeaderboardRequest(String playerName, Integer topPlayers) {
        super("requestLeaderboard");
        this.playerName = playerName;
        this.topPlayers = topPlayers;
    }

    public static RequestLeaderboardRequest forAllPlayers() {
        return new RequestLeaderboardRequest(null, null);
    }

    public static RequestLeaderboardRequest forTopPlayers(Integer topPlayers) {
        return new RequestLeaderboardRequest(null, topPlayers);
    }

    public static RequestLeaderboardRequest forPlayer(String playerName) {
        return new RequestLeaderboardRequest(playerName, null);
    }

    public String getPlayerName() {
        return playerName;
    }

    public Integer getTopPlayers() {
        return topPlayers;
    }
}

