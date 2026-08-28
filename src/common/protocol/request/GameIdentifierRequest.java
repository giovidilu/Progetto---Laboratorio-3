package common.protocol.request;

public abstract class GameIdentifierRequest extends Request {
    private final Integer gameId;

    protected GameIdentifierRequest(String operation, int gameId) {
        super(operation);
        this.gameId = gameId;
    }

    protected GameIdentifierRequest(String operation) {
        super(operation);
        this.gameId = null;
    }

    public Integer getGameId() {
        return gameId;
    }

    public boolean isCurrentGame() {
        return gameId == null;
    }
}