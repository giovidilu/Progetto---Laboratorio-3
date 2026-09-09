package common.protocol.request;

/**
 * Richiesta di consultazione della classifica globale o parziale degli utenti.
 * <p>
 * L'istanziazione avviene tramite factory method dedicati per coprire le tre modalità ammesse
 * da specifica: classifica integrale, podio/top-K giocatori o posizione di un utente specifico.
 * Immutabile e thread-safe.
 */
public class RequestLeaderboardRequest extends Request {
    private final String playerName;
    private final Integer topPlayers;

    /**
     * Costruttore privato. L'istanziazione avviene tramite i factory method statici.
     *
     * @param playerName eventuale username di cui richiedere la posizione
     * @param topPlayers eventuale limite K dei migliori punteggi
     */
    private RequestLeaderboardRequest(String playerName, Integer topPlayers) {
        super("requestLeaderboard");
        this.playerName = playerName;
        this.topPlayers = topPlayers;
    }

    /**
     * Costruisce la richiesta per l'intera classifica di tutti gli utenti registrati.
     *
     * @return istanza di {@link RequestLeaderboardRequest} per la classifica completa
     */
    public static RequestLeaderboardRequest forAllPlayers() {
        return new RequestLeaderboardRequest(null, null);
    }

    /**
     * Costruisce la richiesta per i primi {@code topPlayers} classificati[cite: 99].
     *
     * @param topPlayers numero massimo di posizioni di testa da includere
     * @return istanza di {@link RequestLeaderboardRequest} configurata per i top K
     */
    public static RequestLeaderboardRequest forTopPlayers(Integer topPlayers) {
        return new RequestLeaderboardRequest(null, topPlayers);
    }

    /**
     * Costruisce la richiesta per determinare il ranking di un determinato giocatore.
     *
     * @param playerName nome utente di cui ottenere il piazzamento
     * @return istanza di {@link RequestLeaderboardRequest} mirata al singolo giocatore
     */
    public static RequestLeaderboardRequest forPlayer(String playerName) {
        return new RequestLeaderboardRequest(playerName, null);
    }

    /** Restituisce l'eventuale nome utente filtrato nella richiesta. */
    public String getPlayerName() {
        return playerName;
    }

    /** Restituisce il numero limite K di giocatori richiesti in testa alla classifica. */
    public Integer getTopPlayers() {
        return topPlayers;
    }
}