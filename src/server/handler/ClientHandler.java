package server.handler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import common.dto.AuthRequest;
import common.dto.UpdateCredentialsRequest;
import common.model.User;
import common.protocol.response.ResponseCode;
import common.protocol.response.ServerResponse;
import server.repository.GameRepository;
import server.repository.UserRepository;
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
    private final Gson gson;
    private final Map<String, CommandHandler> commandMap;

    private String loggedInUsername;

    public ClientHandler(Socket socket, UserRepository userRepository, GameRepository gameRepository, SessionManager sessionManager) {
        this.socket = socket;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.sessionManager = sessionManager;
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
        return ServerResponse.failWithMessage(ResponseCode.INTERNAL_SERVER_ERROR, "handleSubmitProposal non ancora implementato");
    }

    private ServerResponse<?> handleRequestGameInfo(JsonObject request) {
        return ServerResponse.failWithMessage(ResponseCode.INTERNAL_SERVER_ERROR, "handleRequestGameInfo non ancora implementato");
    }

    private ServerResponse<?> handleRequestGameStats(JsonObject request) {
        return ServerResponse.failWithMessage(ResponseCode.INTERNAL_SERVER_ERROR, "handleRequestGameStats non ancora implementato");
    }

    private ServerResponse<?> handleRequestLeaderboard(JsonObject request) {
        return ServerResponse.failWithMessage(ResponseCode.INTERNAL_SERVER_ERROR, "handleRequestLeaderboard non ancora implementato");
    }

    private ServerResponse<?> handleRequestPlayerStats(JsonObject request) {
        return ServerResponse.failWithMessage(ResponseCode.INTERNAL_SERVER_ERROR, "handleRequestPlayerStats non ancora implementato");
    }
}