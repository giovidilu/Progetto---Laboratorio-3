package common.protocol;

/**
 * Definisce i letterali stringa associati ai comandi di protocollo supportati dal sistema.
 * <p>
 * Corrispondono ai valori ammessi per il campo {@code operation} nei messaggi JSON di richiesta.
 * Classe di sole costanti, non istanziabile e thread-safe.
 */
public final class OperationType {

    private OperationType() {
    }

    public static final String REGISTER = "register";
    public static final String LOGIN = "login";
    public static final String LOGOUT = "logout";
    public static final String SUBMIT_PROPOSAL = "submitProposal";
    public static final String UPDATE_CREDENTIALS = "updateCredentials";
    public static final String REQUEST_GAME_INFO = "requestGameInfo";
    public static final String REQUEST_GAME_STATS = "requestGameStats";
    public static final String REQUEST_LEADERBOARD = "requestLeaderboard";
    public static final String REQUEST_PLAYER_STATS = "requestPlayerStats";
}