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
import common.protocol.response.payload.PlayerStatsPayload;
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Task worker eseguito su thread pool per gestire l'intero ciclo di vita di una connessione client TCP.
 * <p>
 * Decodifica i messaggi JSON ricevuti, esegue il dispatching ai metodi handler appropriati tramite
 * tabella di comandi e invia le risposte di protocollo. Gestisce lo stato locale di autenticazione
 * del socket garantendo la pulizia della sessione e la chiusura delle risorse al momento della disconnessione.
 * Ogni istanza è confinata sul singolo thread che la esegue.
 */
public class ClientHandler implements Runnable {
    private static final boolean DEBUG = false;

    private final Socket socket;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final SessionManager sessionManager;
    private final GameManager gameManager;
    

    private final Gson gson;
    private final Map<String, CommandHandler> commandMap;

    private String loggedInUsername;

    /**
     * Costruisce l'handler per il socket client collegando i servizi e repository condivisi.
     *
     * @param socket socket TCP associato al client connesso
     * @param userRepository repository condiviso degli utenti
     * @param gameRepository repository condiviso dei record di gioco
     * @param sessionManager gestore delle sessioni attive e degli endpoint UDP
     * @param gameManager coordinatore della logica di gioco globale
     */
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

    /**
     * Registra i riferimenti a metodo per ciascuna operazione supportata nella tabella di dispatching.
     */
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

    /**
     * Ciclo principale di lettura riga per riga (stream delimitato da newline), elaborazione
     * dei comandi e scrittura delle risposte sul canale TCP.
     * <p>
     * Al termine garantisce il rilascio della sessione e la chiusura ordinata del socket.
     */
    @Override
    public void run() {
        System.out.println("[WORKER] Gestione client avviata su thread: " + Thread.currentThread().getName());
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                if (DEBUG) {
                    System.out.println("[SERVER DEBUG] Richiesta ricevuta: " + line);
                }
                ServerResponse<?> response = processRequest(line);
                if (DEBUG) {
                    System.out.println("[SERVER DEBUG] Invio risposta: " + gson.toJson(response));
                }
                out.println(gson.toJson(response));
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Disconnessione anomala del client (" + socket.getRemoteSocketAddress() + "): " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Valida sintatticamente la stringa JSON e ne indirizza l'elaborazione al gestore associato.
     * <p>
     * Cattura e mappa eventuali errori sintattici in codici di errore BAD_REQUEST o INTERNAL_SERVER_ERROR.
     *
     * @param rawJson riga di testo ricevuta dal client
     * @return risposta formattata prodotta dall'handler o risposta di errore
     */
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

    /**
     * Libera le risorse di rete e rimuove l'eventuale sessione attiva dal {@link SessionManager}.
     */
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

    /**
     * Gestisce la richiesta di registrazione di un nuovo utente generando salt e password hash.
     *
     * @param request payload JSON della richiesta contenente username e password
     * @return esito SUCCESS se registrato, USERNAME_ALREADY_TAKEN se duplicato, o BAD_REQUEST se non valido
     */
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

    /**
     * Autentica il client verificando hash e credenziali, registrando l'endpoint UDP in sessione.
     *
     * @param request payload JSON contenente username, password e porta UDP del client
     * @return esito SUCCESS o codice di errore (INVALID_CREDENTIALS, BAD_REQUEST se già connesso
     */
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

        InetSocketAddress udpEndpoint = null;
        if (authReq.getUdpPort() != null && authReq.getUdpPort() > 0 && authReq.getUdpPort() <= 65535) {
            udpEndpoint = new InetSocketAddress(this.socket.getInetAddress(), authReq.getUdpPort());
        }

        boolean sessionAcquired = sessionManager.login(user.getUsername(), udpEndpoint);
        if (!sessionAcquired) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Utente già connesso su un'altra sessione.");
        }

        this.loggedInUsername = user.getUsername();
        
        return ServerResponse.successWithoutPayload(ResponseCode.SUCCESS);
    }

    /**
     * Elabora l'aggiornamento sicuro di username e/o password previa verifica delle credenziali attuali.
     *
     * @param request payload JSON con vecchie e nuove credenziali
     * @return risposta di conferma o errore relativo a credenziali errate o nome utente occupato
     */
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
            InetSocketAddress existingEndpoint = sessionManager.getUdpEndpoint(updateReq.getOldUsername());
            sessionManager.logout(updateReq.getOldUsername());
            sessionManager.login(newUsername, existingEndpoint);
            this.loggedInUsername = newUsername;
        }

        return ServerResponse.successWithoutPayload(ResponseCode.SUCCESS);
    }

    /**
     * Termina la sessione dell'utente connesso liberando la risorsa sul SessionManager.
     *
     * @param request payload JSON della richiesta di logout
     * @return esito SUCCESS se disconnesso, NOT_LOGGED_IN se il client non era autenticato
     */
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

    /**
     * Sottomette a GameManager la proposta di 4 parole per la partita corrente da parte dell'utente.
     *
     * @param request payload JSON contenente l'array di parole proposte
     * @return payload con ProposalResult se valido, o codice di errore di protocollo
     */
    private ServerResponse<?> handleSubmitProposal(JsonObject request) {
        if (this.loggedInUsername == null) {
            return ServerResponse.failWithMessage(ResponseCode.NOT_LOGGED_IN, "Operazione non consentita: utente non autenticato.");
        }

        SubmitProposalRequest proposalReq = gson.fromJson(request, SubmitProposalRequest.class);
        if (proposalReq == null || proposalReq.getWords() == null) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Formato richiesta non valido: campo 'words' mancante o nullo.");
        }

        ProposalResult result = this.gameManager.submitProposal(this.loggedInUsername, proposalReq.getWords());

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

    /**
     * Interroga lo stato della partita corrente o archiviata per il giocatore autenticato.
     *
     * @param request payload JSON con l'eventuale gameId desiderato
     * @return risposta contenente {@link GameInfoPayload}, oppure GAME_NOT_FOUND
     */
    private ServerResponse<?> handleRequestGameInfo(JsonObject request) {
        if (this.loggedInUsername == null) {
            return ServerResponse.failWithMessage(ResponseCode.NOT_LOGGED_IN, "Operazione non consentita: utente non autenticato.");
        }

        GameQueryRequest queryReq = gson.fromJson(request, GameQueryRequest.class);
        Integer gameId = (queryReq != null) ? queryReq.getGameId() : null;

        if(gameId != null && gameId == 0){
            gameId = null;
        }

        GameInfoPayload payload = this.gameManager.getGameInfoForPlayer(this.loggedInUsername, gameId);
        if (payload == null) {
            return ServerResponse.failWithMessage(
                ResponseCode.GAME_NOT_FOUND,
                "Partita non trovata per l'ID specificato: " + gameId
            );
        }

        return ServerResponse.successWithPayload(ResponseCode.SUCCESS, payload);
    }

    /**
     * Recupera le statistiche aggregate di una partita specifica o di quella in corso.
     *
     * @param request payload JSON con l'eventuale gameId
     * @return risposta contenente {@link GameStatsPayload} o errore
     */
    private ServerResponse<?> handleRequestGameStats(JsonObject request) {
        if(this.loggedInUsername == null){
            return ServerResponse.failWithMessage(ResponseCode.NOT_LOGGED_IN, "Operazione non consentita: utente non autenticato. ");
        }

        GameQueryRequest queryReq = gson.fromJson(request, GameQueryRequest.class);
        Integer gameId = (queryReq != null) ? queryReq.getGameId() : null;

        if(gameId != null && gameId == 0){
            gameId = null;
        }

        GameStatsPayload payload = this.gameManager.getGameStats(gameId);
        if(payload == null){
            return ServerResponse.failWithMessage(
                ResponseCode.GAME_NOT_FOUND,
                "Partita non trovata per l'ID specificato: " + gameId
            );
        }

        return ServerResponse.successWithPayload(ResponseCode.SUCCESS, payload);
    }

    /**
     * Calcola ed estrae i dati della classifica globale, dei primi K utenti o del singolo utente.
     *
     * @param request payload JSON contenente i criteri di ordinamento/filtro
     * @return risposta contenente {@link LeaderboardPayload} o PLAYER_NOT_FOUND
     */
    private ServerResponse<?> handleRequestLeaderboard(JsonObject request) {
        if (this.loggedInUsername == null) {
            return ServerResponse.failWithMessage(ResponseCode.NOT_LOGGED_IN, "Operazione non consentita: utente non autenticato.");
        }

        common.dto.LeaderboardRequest leaderboardReq = gson.fromJson(request, common.dto.LeaderboardRequest.class);
        if (leaderboardReq == null || !leaderboardReq.isValid()) {
            return ServerResponse.failWithMessage(ResponseCode.BAD_REQUEST, "Parametri della richiesta classifica non validi o malformati.");
        }

        LeaderboardPayload payload = this.gameManager.getLeaderboard(
            leaderboardReq.getTopPlayer(), 
            leaderboardReq.getPlayerName()
        );

        if (payload == null && leaderboardReq.getPlayerName() != null && !leaderboardReq.getPlayerName().isBlank()) {
            return ServerResponse.failWithMessage(
                ResponseCode.PLAYER_NOT_FOUND, 
                "Giocatore non trovato nella classifica: " + leaderboardReq.getPlayerName()
            );
        }

        return ServerResponse.successWithPayload(ResponseCode.SUCCESS, payload);
    }

    /**
     * Recupera le statistiche personali storiche del giocatore attualmente loggato.
     *
     * @param request payload JSON della richiesta
     * @return risposta contenente {@link PlayerStatsPayload} con percentuali e istogramma
     */
    private ServerResponse<?> handleRequestPlayerStats(JsonObject request) {
        if (this.loggedInUsername == null) {
            return ServerResponse.failWithMessage(ResponseCode.NOT_LOGGED_IN, "Operazione non consentita: utente non autenticato.");
        }

        PlayerStatsPayload payload = this.gameManager.getPlayerStats(this.loggedInUsername);
        if (payload == null) {
            return ServerResponse.failWithMessage(
                ResponseCode.INTERNAL_SERVER_ERROR,
                "Impossibile recuperare le statistiche personali per l'utente: " + this.loggedInUsername
            );
        }

        return ServerResponse.successWithPayload(ResponseCode.SUCCESS, payload);
    }
}