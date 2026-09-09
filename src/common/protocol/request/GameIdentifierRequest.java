package common.protocol.request;

/**
 * Classe base astratta per richieste indirizzate a una specifica partita o alla partita attiva.
 * <p>
 * Un identificativo {@code null} segnala implicitamente l'interesse verso la sessione corrente.
 * Immutabile e thread-safe.
 */
public abstract class GameIdentifierRequest extends Request {
    private final Integer gameId;

    /**
     * Costruisce la richiesta indirizzata a una partita specifica.
     *
     * @param operation nome del comando di protocollo
     * @param gameId identificativo della partita desiderata
     */
    protected GameIdentifierRequest(String operation, int gameId) {
        super(operation);
        this.gameId = gameId;
    }

    /**
     * Costruisce la richiesta indirizzata implicitamente alla partita attiva.
     *
     * @param operation nome del comando di protocollo
     */
    protected GameIdentifierRequest(String operation) {
        super(operation);
        this.gameId = null;
    }

    /** Restituisce l'ID del gioco richiesto, oppure {@code null} se relativo alla partita corrente. */
    public Integer getGameId() {
        return gameId;
    }

    /**
     * Verifica se la richiesta è rivolta alla partita in corso.
     * <p>
     * Query pura.
     *
     * @return {@code true} se {@code gameId} è nullo, {@code false} se punta a un id specifico
     */
    public boolean isCurrentGame() {
        return gameId == null;
    }
}