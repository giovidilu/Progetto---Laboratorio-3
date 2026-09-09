package common.protocol.response.payload;

import java.util.Objects;

/**
 * Payload veicolato tramite notifica asincrona UDP alla conclusione di una partita globale.
 * <p>
 * Riassume le soluzioni integrali dei gruppi, i risultati individuali dell'utente nel round
 * e le statistiche globali consolidate di tutti i partecipanti.
 * Immutabile e thread-safe.
 */
public class GameFinishedNotificationPayload {

    private final int gameId;
    private final GameInfoPayload gameInfo;
    private final GameStatsPayload gameStats;

    /**
     * Inizializza il payload aggregato verificando la non nullità dei sotto-resoconti.
     *
     * @param gameId identificativo univoco della partita conclusa
     * @param gameInfo snapshot contenente la soluzione completa e il resoconto personale
     * @param gameStats statistiche aggregate di partecipazione della partita
     * @throws NullPointerException se {@code gameInfo} o {@code gameStats} sono nulli
     */
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