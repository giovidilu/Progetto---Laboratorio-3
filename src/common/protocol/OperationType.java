package common.protocol;

/**
 * Costanti per le stringhe identificative delle operazioni
 * supportate dal protocollo di comunicazione JSON (Sezione 5).
 */
public final class OperationType {

    private OperationType() {
        // Costruttore privato per impedire l'istanziazione
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