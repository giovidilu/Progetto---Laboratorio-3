package server.handler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import common.dto.AuthRequest;
import common.dto.GameQueryRequest;
import common.dto.UpdateCredentialsRequest;
import common.dto.SubmitProposalRequest;
import common.model.Game;
import common.model.ProposalResult;
import common.model.User;
import common.protocol.response.ResponseCode;
import common.protocol.response.ServerResponse;
import common.protocol.response.payload.GameInfoPayload;
import common.protocol.response.payload.GameStatsPayload;
import common.protocol.response.payload.LeaderboardPayload;
import server.repository.GameRepository;
import server.repository.UserRepository;
import server.service.GameManager;
import server.service.SessionManager;
import server.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final SessionManager sessionManager;
    private final GameManager gameManager;

    private final Gson gson;
    private final Map<String, CommandHandler> commandMap;

    private String loggedInUsername;

    public ClientHandler(Socket socket, UserRepository userRepository, GameRepository gameRepository, SessionManager sessionManager, GameManager gameManager) {
        this.socket = socket;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.sessionManager = sessionManager;
        this.gameManager = gameManager;
        this.gson = new Gson();
        this.commandMap = new HashMap<>();

        initCommandMap();
    }

    private void initCommandMap() {
        commandMap.put("register", this::handleRegister);
        commandMap.put("login", this::handleLogin);
        commandMap.put("updateCredentials", this::handleUpdateCredentials);
        commandMap.put("logout", this::handleLogout);

        commandMap.put("submitProposal", this::handleSubmitProposal);
        commandMap.put("requestGameInfo", this::handleRequestGameInfo);
        commandMap.put("requestGameStats", this::handleRequestGameStats);

        commandMap.put("requestLeaderboard", this::handleRequestLeaderboard);
        commandMap.put("requestPlayerStats", this::handleRequestPlayerStats);
    }

    @Override
    public void run() {
        System.out.println("[WORKER] Gestione client avviata su thread: " + Thread.currentThread().getName());
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[SERVER DEBUG] Richiesta ricevuta: " + line);
                ServerResponse<?> response = processRequest(line);
                System.out.println("[SERVER DEBUG] Invio risposta: " + gson.toJson(response));
                out.println(gson.toJson(response));
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Disconnessione anomala del client (" + socket.getRemoteSocketAddress() + "): " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private ServerResponse<?> processRequest(String rawJson) {
        try {
            JsonElement element = JsonParser.parseString(rawJson);
            if (!element.isJsonObject()) {
                return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Formato richiesta non valido: atteso un oggetto JSON.");
            }

            JsonObject requestJson = element.getAsJsonObject();
            if (!requestJson.has("operation") || requestJson.get("operation").isJsonNull()) {
                return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Parametro obbligatorio 'operation' mancante.");
            }

            String operation = requestJson.get("operation").getAsString();
            CommandHandler handler = commandMap.get(operation);

            if (handler == null) {
                return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Operazione sconosciuta: " + operation);
            }

            return handler.handle(requestJson);
        } catch (JsonSyntaxException e) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Sintassi JSON non valida: " + e.getMessage());
        } catch (Exception e) {
            return ServerResponse.failWithMessage(ResponseCode.INTERNAL_SERVER_ERROR, "Errore interno durante l'elaborazione della richiesta: " + e.getMessage());
        }
    }

    private void cleanup() {
        if (loggedInUsername != null) {
            sessionManager.logout(loggedInUsername);
            System.out.println("[SESSION] Utente disconnesso: " + loggedInUsername);
            loggedInUsername = null;
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Errore durante la chiusura del socket: " + e.getMessage());
        }
    }

    // --- Gestori delle operazioni ---

    private ServerResponse<?> handleRegister(JsonObject request) {
        AuthRequest authReq = gson.fromJson(request, AuthRequest.class);
        if (authReq == null || !authReq.isValid()) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Parametri di registrazione non validi o mancanti.");
        }

        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(authReq.getPsw(), salt);

        User newUser = new User(authReq.getUsername(), passwordHash, salt);

        boolean added = userRepository.addUser(newUser);
        if (!added) {
            return ServerResponse.failWithMessage(ResponseCode.USERNAME_ALREADY_TAKEN, "Username già registrato.");
        }
        return ServerResponse.successWithoutPayload(ResponseCode.SUCCESS);
    }

    private ServerResponse<?> handleLogin(JsonObject request) {
        if (this.loggedInUsername != null) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Client già autenticato con l'utente: " + this.loggedInUsername);
        }

        AuthRequest authReq = gson.fromJson(request, AuthRequest.class);
        if (authReq == null || !authReq.isValid()) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Parametri di login non validi o mancanti.");
        }

        User user = userRepository.getUser(authReq.getUsername());
        if (user == null) {
            return ServerResponse.failWithMessage(ResponseCode.INVALID_CREDENTIALS, "Credenziali non valide.");
        }

        String calculatedHash = PasswordUtil.hashPassword(authReq.getPsw(), user.getSalt());
        if (!calculatedHash.equals(user.getPasswordHash())) {
            return ServerResponse.failWithMessage(ResponseCode.INVALID_CREDENTIALS, "Credenziali non valide.");
        }

        boolean sessionAcquired = sessionManager.login(user.getUsername());
        if (!sessionAcquired) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Utente già connesso su un'altra sessione.");
        }

        this.loggedInUsername = user.getUsername();
        
        // Al momento restituiamo successo; verrà integrato LoginPayload con i dati di GameManager
        return ServerResponse.successWithoutPayload(ResponseCode.SUCCESS);
    }

    private ServerResponse<?> handleUpdateCredentials(JsonObject request) {
        UpdateCredentialsRequest updateReq = gson.fromJson(request, UpdateCredentialsRequest.class);
        if (updateReq == null || !updateReq.isValid()) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Parametri non validi per l'aggiornamento credenziali.");
        }

        if (this.loggedInUsername != null && !this.loggedInUsername.equals(updateReq.getOldUsername())) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Non è consentito modificare credenziali di un altro utente.");
        }

        User user = userRepository.getUser(updateReq.getOldUsername());
        if (user == null) {
            return ServerResponse.failWithMessage(ResponseCode.INVALID_CREDENTIALS, "Credenziali non valide.");
        }

        String oldCalculatedHash = PasswordUtil.hashPassword(updateReq.getOldPsw(), user.getSalt());
        if (!oldCalculatedHash.equals(user.getPasswordHash())) {
            return ServerResponse.failWithMessage(ResponseCode.INVALID_CREDENTIALS, "Credenziali non valide.");
        }

        String newPasswordHash = null;
        String newSalt = null;
        if (updateReq.getNewPsw() != null && !updateReq.getNewPsw().isBlank()) {
            newSalt = PasswordUtil.generateSalt();
            newPasswordHash = PasswordUtil.hashPassword(updateReq.getNewPsw(), newSalt);
        }

        String newUsername = updateReq.getNewUsername();

        boolean success = userRepository.updateCredentials(
            updateReq.getOldUsername(),
            newUsername,
            newPasswordHash,
            newSalt
        );

        if (!success) {
            return ServerResponse.failWithMessage(ResponseCode.USERNAME_ALREADY_TAKEN, "Impossibile aggiornare: il nuovo username specificato è già in uso.");
        }

        if (newUsername != null && !newUsername.equals(updateReq.getOldUsername()) && this.loggedInUsername != null) {
            sessionManager.logout(updateReq.getOldUsername());
            sessionManager.login(newUsername);
            this.loggedInUsername = newUsername;
        }

        return ServerResponse.successWithoutPayload(ResponseCode.SUCCESS);
    }

    private ServerResponse<?> handleLogout(JsonObject request) {
        if (this.loggedInUsername == null) {
            return ServerResponse.failWithMessage(ResponseCode.NOT_LOGGED_IN, "Operazione non valida: nessun utente autenticato su questa connessione.");
        }

        sessionManager.logout(this.loggedInUsername);

        String previousUser = this.loggedInUsername;
        this.loggedInUsername = null;

        System.out.println("[SESSION] Logout eseguito per: " + previousUser);
        return ServerResponse.successWithoutPayload(ResponseCode.SUCCESS);
    }

    private ServerResponse<?> handleSubmitProposal(JsonObject request) {
        // 1. Controllo di autenticazione
        if (this.loggedInUsername == null) {
            return ServerResponse.failWithMessage(ResponseCode.NOT_LOGGED_IN, "Operazione non consentita: utente non autenticato.");
        }

        // 2. Deserializzazione e validazione di protocollo
        SubmitProposalRequest proposalReq = gson.fromJson(request, SubmitProposalRequest.class);
        if (proposalReq == null || proposalReq.getWords() == null) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Formato richiesta non valido: campo 'words' mancante o nullo.");
        }

        // 3. Esecuzione della proposta su GameManager
        ProposalResult result = this.gameManager.submitProposal(this.loggedInUsername, proposalReq.getWords());

        // 4. Mappatura MoveOutcome -> ServerResponse
        switch (result.getMoveOutcome()) {
            case MALFORMED:
                return ServerResponse.failWithMessage(
                    ResponseCode.MALFORMED_PROPOSAL,
                    "Proposta malformata: le parole non appartengono al set valido o sono già state indovinate."
                );
            case ALREADY_COMPLETED:
                return ServerResponse.failWithMessage(
                    ResponseCode.BAD_REQUEST,
                    "Partita già conclusa per questo utente o tempo scaduto."
                );
            case CORRECT:
            case WRONG:
            default:
                return ServerResponse.successWithPayload(ResponseCode.SUCCESS, result);
        }
    }

    private ServerResponse<?> handleRequestGameInfo(JsonObject request) {
       // 1. Controllo di autenticazione
        if (this.loggedInUsername == null) {
            return ServerResponse.failWithMessage(ResponseCode.NOT_LOGGED_IN, "Operazione non consentita: utente non autenticato.");
        }

        // 2. Deserializzazione tramite GameQueryRequest (gameId è null se non presente nel JSON)
        GameQueryRequest queryReq = gson.fromJson(request, GameQueryRequest.class);
        Integer gameId = (queryReq != null) ? queryReq.getGameId() : null;

        // 3. Recupero dello stato della partita (attiva se gameId == null, storica altrimenti)
        GameInfoPayload payload = this.gameManager.getGameInfoForPlayer(this.loggedInUsername, gameId);
        if (payload == null) {
            return ServerResponse.failWithMessage(
                ResponseCode.GAME_NOT_FOUND,
                "Partita non trovata per l'ID specificato: " + gameId
            );
        }

        // 4. Invio delle informazioni di gioco
        return ServerResponse.successWithPayload(ResponseCode.SUCCESS, payload);
    }

    private ServerResponse<?> handleRequestGameStats(JsonObject request) {
        if(this.loggedInUsername == null){
            return ServerResponse.failWithMessage(ResponseCode.NOT_LOGGED_IN, "Operazione non consentita: utente non autenticato. ");
        }

        GameQueryRequest queryReq = gson.fromJson(request, GameQueryRequest.class);
        Integer gameId = (queryReq != null) ? queryReq.getGameId() : null;

        GameStatsPayload payload = this.gameManager.getGameStats(gameId);
        if(payload == null){
            return ServerResponse.failWithMessage(
                ResponseCode.GAME_NOT_FOUND,
                "Partita non trovata per l'ID specificato: " + gameId

            );
        }

        return ServerResponse.successWithPayload(ResponseCode.SUCCESS, payload);
    }

    private ServerResponse<?> handleRequestLeaderboard(JsonObject request) {
        // 1. Controllo di autenticazione
        if (this.loggedInUsername == null) {
            return ServerResponse.failWithMessage(ResponseCode.NOT_LOGGED_IN, "Operazione non consentita: utente non autenticato.");
        }

        // 2. Deserializzazione e validazione di protocollo tramite il DTO LeaderboardRequest
        common.dto.LeaderboardRequest leaderboardReq = gson.fromJson(request, common.dto.LeaderboardRequest.class);
        if (leaderboardReq == null || !leaderboardReq.isValid()) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Parametri della richiesta classifica non validi o malformati.");
        }

        // 3. Interrogazione del GameManager utilizzando il metodo corretto getTopPlayer()
        LeaderboardPayload payload = this.gameManager.getLeaderboard(
            leaderboardReq.getTopPlayer(), 
            leaderboardReq.getPlayerName()
        );

        // 4. Controllo sull'esistenza del giocatore specifico (se era stato richiesto)
        if (payload == null && leaderboardReq.getPlayerName() != null && !leaderboardReq.getPlayerName().isBlank()) {
            return ServerResponse.failWithMessage(
                ResponseCode.PLAYER_NOT_FOUND, 
                "Giocatore non trovato nella classifica: " + leaderboardReq.getPlayerName()
            );
        }

        // 5. Restituzione del successo con il payload della classifica
        return ServerResponse.successWithPayload(ResponseCode.SUCCESS, payload);
    }

    private ServerResponse<?> handleRequestPlayerStats(JsonObject request) {
        return ServerResponse.failWithMessage(ResponseCode.INTERNAL_SERVER_ERROR, "handleRequestPlayerStats non ancora implementato");
    }
}