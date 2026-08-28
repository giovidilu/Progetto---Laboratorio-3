package common.protocol.request;

public class RequestGameInfoRequest extends GameIdentifierRequest {
    public RequestGameInfoRequest(int gameId) {
        super("requestGameInfo", gameId);
    }

    public RequestGameInfoRequest() {
        super("requestGameInfo");
    }
}