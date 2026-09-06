package common.protocol.response.payload;

import java.util.Objects;

/**
 * Payload per la notifica asincrona UDP di fine partita.
 * Combina l'esito della partita (con soluzioni e resoconto individuale)
 * e le statistiche aggregate finali del round.
 */
public class GameFinishedNotificationPayload {

    private final int gameId;
    private final GameInfoPayload gameInfo;
    private final GameStatsPayload gameStats;

    public GameFinishedNotificationPayload(int gameId, GameInfoPayload gameInfo, GameStatsPayload gameStats) {
        this.gameId = gameId;
        this.gameInfo = Objects.requireNonNull(gameInfo, "gameInfo non può essere null");
        this.gameStats = Objects.requireNonNull(gameStats, "gameStats non può essere null");
    }

    public int getGameId() {
        return gameId;
    }

    public GameInfoPayload getGameInfo() {
        return gameInfo;
    }

    public GameStatsPayload getGameStats() {
        return gameStats;
    }
}