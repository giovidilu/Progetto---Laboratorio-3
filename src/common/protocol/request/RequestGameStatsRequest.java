package common.protocol.request;

public class RequestGameStatsRequest extends GameIdentifierRequest {
    public RequestGameStatsRequest(int gameId) {
        super("requestGameStats", gameId);
    }

    public RequestGameStatsRequest() {
        super("requestGameStats");
    }
}