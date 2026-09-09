package common.protocol;

import common.protocol.request.LoginRequest;
import common.protocol.request.LogoutRequest;
import common.protocol.request.RegisterRequest;
import common.protocol.request.Request;
import common.protocol.request.RequestGameInfoRequest;
import common.protocol.request.RequestGameStatsRequest;
import common.protocol.request.RequestLeaderboardRequest;
import common.protocol.request.RequestPlayerStatsRequest;
import common.protocol.request.SubmitProposalRequest;
import common.protocol.request.UpdateCredentialsRequest;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Parser per i messaggi JSON di richiesta provenienti dai client.
 * <p>
 * Identifica la tipologia di richiesta tramite il discriminatore {@code operation}
 * e ne deserializza il payload nel corrispondente sottotipo di {@link Request}.
 * Stateless e thread-safe.
 */
public class RequestParser {

    private static final Gson gson = new Gson();
    
    /**
     * Converte una stringa JSON nella specifica istanza di {@link Request} corrispondente.
     * <p>
     * Query pura (nessun effetto collaterale).
     *
     * @param jsonString stringa JSON inviata dal client
     * @return istanza tipizzata della richiesta deserializzata
     * @throws IllegalArgumentException se l'operazione non è riconosciuta, il campo {@code operation} è assente
     *                                  o la stringa JSON è sintatticamente malformata
     */
    public static Request parseRequest(String jsonString)throws IllegalArgumentException{
        
        JsonElement stringElement = JsonParser.parseString(jsonString);
        JsonObject stringObject = stringElement.getAsJsonObject();
        String operation = stringObject.get("operation").getAsString();
        
        switch (operation) {
            case "register":
                return gson.fromJson(stringObject, RegisterRequest.class);
            case "updateCredentials":
                return gson.fromJson(stringObject, UpdateCredentialsRequest.class);
            case "login":
                return gson.fromJson(stringObject, LoginRequest.class);
            case "logout":
                return gson.fromJson(stringObject, LogoutRequest.class);
            case "submitProposal":
                return gson.fromJson(stringObject, SubmitProposalRequest.class);
            case "requestGameInfo":
                return gson.fromJson(stringObject, RequestGameInfoRequest.class);
            case "requestGameStats":
                return gson.fromJson(stringObject, RequestGameStatsRequest.class);
            case "requestLeaderboard":
                return gson.fromJson(stringObject, RequestLeaderboardRequest.class);
            case "requestPlayerStats":
                return gson.fromJson(stringObject, RequestPlayerStatsRequest.class);
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }
}