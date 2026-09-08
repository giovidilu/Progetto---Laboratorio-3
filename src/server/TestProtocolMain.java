package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Suite di test di conformità per il Protocollo di Comunicazione (Sezione 5).
 * Verifica la presenza dei campi obbligatori, la corretta gestione dei campi omessi (BAD_REQUEST)
 * e la restituzione di tutti i codici di errore previsti dalla specifica.
 */
public class TestProtocolMain {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    /**
     * Invia un comando JSON sulla connessione TCP e verifica che il codice di risposta
     * corrisponda esattamente a quello atteso dalla specifica.
     */
    private static void assertResponseCode(PrintWriter out, BufferedReader in, 
                                          String rawJson, String expectedCode, 
                                          String description) throws Exception {
        System.out.println("\n[TEST] " + description);
        System.out.println("  -> Invio    : " + rawJson);
        
        out.println(rawJson);
        out.flush();
        
        String response = in.readLine();
        System.out.println("  <- Ricevuto : " + response);

        // Verifica allineata al campo effettivamente prodotto dalla classe ServerResponse
        if (response != null && response.contains("\"status\":\"" + expectedCode + "\"")) {
            System.out.println("  [PASS] Riscontrato codice atteso: " + expectedCode);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] Codice atteso: " + expectedCode + " | Risposta ottenuta: " + response);
            testsFailed++;
        }
    }

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8888; // Configurare sulla porta TCP del proprio server

        System.out.println("================================================================");
        System.out.println(" AVVIO TEST DI CONFORMITÀ PROTOCOLLO - SEZIONE 5 (SPECIFICA) ");
        System.out.println("================================================================");

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            String userA = "protoUserA_" + System.currentTimeMillis();
            String userB = "protoUserB_" + System.currentTimeMillis();
            String psw = "Password123!";

            // -------------------------------------------------------------
            // BLOCCO 1: OPERAZIONI SENZA AUTENTICAZIONE (Stato iniziale)
            // -------------------------------------------------------------
            System.out.println("\n--- BLOCCO 1: Verifiche preliminari (client non autenticato) ---");
            
            // Logout senza sessione attiva
            assertResponseCode(out, in, 
                "{\"operation\":\"logout\"}", 
                "NOT_LOGGED_IN", "Logout senza aver effettuato il login");

            // Richiesta statistiche utente senza sessione attiva
            assertResponseCode(out, in, 
                "{\"operation\":\"requestPlayerStats\"}", 
                "NOT_LOGGED_IN", "requestPlayerStats da non autenticato");

            // Invio proposta da non autenticato
            assertResponseCode(out, in, 
                "{\"operation\":\"submitProposal\",\"words\":[\"W1\",\"W2\",\"W3\",\"W4\"]}", 
                "NOT_LOGGED_IN", "submitProposal da non autenticato");

            // -------------------------------------------------------------
            // BLOCCO 2: ROBUSTEZZA SINTATTICA E CAMPI OBBLIGATORI (BAD_REQUEST)
            // -------------------------------------------------------------
            System.out.println("\n--- BLOCCO 2: Omissione campi obbligatori (BAD_REQUEST) ---");

            // JSON privo dell'operazione
            assertResponseCode(out, in, 
                "{\"username\":\"test\"}", 
                "BAD_REQUEST", "Richiesta priva del campo 'operation'");

            // Operazione inesistente
            assertResponseCode(out, in, 
                "{\"operation\":\"unknownOp\"}", 
                "BAD_REQUEST", "Operazione non supportata");

            // register senza 'psw'
            assertResponseCode(out, in, 
                "{\"operation\":\"register\",\"username\":\"" + userA + "\"}", 
                "BAD_REQUEST", "register privo del campo obbligatorio 'psw'");

            // register senza 'username'
            assertResponseCode(out, in, 
                "{\"operation\":\"register\",\"psw\":\"" + psw + "\"}", 
                "BAD_REQUEST", "register privo del campo obbligatorio 'username'");

            // login senza 'psw'
            assertResponseCode(out, in, 
                "{\"operation\":\"login\",\"username\":\"" + userA + "\"}", 
                "BAD_REQUEST", "login privo del campo obbligatorio 'psw'");

            // updateCredentials senza 'oldUsername'
            assertResponseCode(out, in, 
                "{\"operation\":\"updateCredentials\",\"oldPsw\":\"" + psw + "\",\"newPsw\":\"new\"}", 
                "BAD_REQUEST", "updateCredentials privo di 'oldUsername'");

            // -------------------------------------------------------------
            // BLOCCO 3: REGISTRAZIONE E LOGIN
            // -------------------------------------------------------------
            System.out.println("\n--- BLOCCO 3: Flusso Registrazione e Autenticazione ---");

            // Registrazione corretta utente A
            assertResponseCode(out, in, 
                "{\"operation\":\"register\",\"username\":\"" + userA + "\",\"psw\":\"" + psw + "\"}", 
                "SUCCESS", "Registrazione regolare di userA");

            // Registrazione corretta utente B (usato poi per collisione credenziali)
            assertResponseCode(out, in, 
                "{\"operation\":\"register\",\"username\":\"" + userB + "\",\"psw\":\"" + psw + "\"}", 
                "SUCCESS", "Registrazione regolare di userB");

            // Doppia registrazione stesso username (userA)
            assertResponseCode(out, in, 
                "{\"operation\":\"register\",\"username\":\"" + userA + "\",\"psw\":\"" + psw + "\"}", 
                "USERNAME_ALREADY_TAKEN", "Registrazione duplicata dello stesso username");

            // Login con password errata
            assertResponseCode(out, in, 
                "{\"operation\":\"login\",\"username\":\"" + userA + "\",\"psw\":\"wrongPassword\"}", 
                "INVALID_CREDENTIALS", "Login con password errata");

            // Login con username non registrato
            assertResponseCode(out, in, 
                "{\"operation\":\"login\",\"username\":\"ghostUser_999\",\"psw\":\"dummy\"}", 
                "INVALID_CREDENTIALS", "Login con utente inesistente");

            // Login valido di userA
            assertResponseCode(out, in, 
                "{\"operation\":\"login\",\"username\":\"" + userA + "\",\"psw\":\"" + psw + "\",\"udpPort\":8889}", 
                "SUCCESS", "Login valido di userA con porta UDP");

            // -------------------------------------------------------------
            // BLOCCO 4: PROPOSTE E PARTITA CORRENTE
            // -------------------------------------------------------------
            System.out.println("\n--- BLOCCO 4: Gestione Partita e Proposte ---");

            // Proposta malformata: campo 'words' mancante
            assertResponseCode(out, in, 
                "{\"operation\":\"submitProposal\"}", 
                "BAD_REQUEST", "submitProposal senza array 'words'");

            // Proposta malformata: meno di 4 parole
            assertResponseCode(out, in, 
                "{\"operation\":\"submitProposal\",\"words\":[\"W1\",\"W2\"]}", 
                "MALFORMED_PROPOSAL", "submitProposal con solo 2 parole");

            // Proposta malformata: parole duplicate nella quadrupla
            assertResponseCode(out, in, 
                "{\"operation\":\"submitProposal\",\"words\":[\"W1\",\"W1\",\"W2\",\"W3\"]}", 
                "MALFORMED_PROPOSAL", "submitProposal con parole duplicate");

            // Proposta malformata: parole non appartenenti alla partita
            assertResponseCode(out, in, 
                "{\"operation\":\"submitProposal\",\"words\":[\"NON_ESISTE_1\",\"NON_ESISTE_2\",\"NON_ESISTE_3\",\"NON_ESISTE_4\"]}", 
                "MALFORMED_PROPOSAL", "submitProposal con parole esterne al set attivo");

            // -------------------------------------------------------------
            // BLOCCO 5: INTERROGAZIONI STATO E STATISTICHE (gameId)
            // -------------------------------------------------------------
            System.out.println("\n--- BLOCCO 5: Richiesta Info e Stats Partita ---");

            // Partita corrente tramite sentinella gameId = 0
            assertResponseCode(out, in, 
                "{\"operation\":\"requestGameInfo\",\"gameId\":0}", 
                "SUCCESS", "requestGameInfo partita corrente con gameId: 0");

            // Partita corrente omettendo del tutto il campo gameId
            assertResponseCode(out, in, 
                "{\"operation\":\"requestGameInfo\"}", 
                "SUCCESS", "requestGameInfo partita corrente con campo gameId omesso");

            // Partita inesistente
            assertResponseCode(out, in, 
                "{\"operation\":\"requestGameInfo\",\"gameId\":99999}", 
                "GAME_NOT_FOUND", "requestGameInfo con gameId inesistente (99999)");

            // Stats partita corrente con gameId = 0
            assertResponseCode(out, in, 
                "{\"operation\":\"requestGameStats\",\"gameId\":0}", 
                "SUCCESS", "requestGameStats partita corrente con gameId: 0");

            // Stats partita corrente con campo omesso
            assertResponseCode(out, in, 
                "{\"operation\":\"requestGameStats\"}", 
                "SUCCESS", "requestGameStats partita corrente con campo gameId omesso");

            // Stats partita inesistente
            assertResponseCode(out, in, 
                "{\"operation\":\"requestGameStats\",\"gameId\":99999}", 
                "GAME_NOT_FOUND", "requestGameStats con gameId inesistente (99999)");

            // -------------------------------------------------------------
            // BLOCCO 6: CLASSIFICA E STATISTICHE UTENTE
            // -------------------------------------------------------------
            System.out.println("\n--- BLOCCO 6: Classifiche e Statistiche Giocatore ---");

            // Classifica generale (tutti i giocatori)
            assertResponseCode(out, in, 
                "{\"operation\":\"requestLeaderboard\",\"topPlayers\":0}", 
                "SUCCESS", "requestLeaderboard generale");

            // Classifica Top-K
            assertResponseCode(out, in, 
                "{\"operation\":\"requestLeaderboard\",\"topPlayers\":3}", 
                "SUCCESS", "requestLeaderboard top 3");

            // Classifica per singolo giocatore esistente
            assertResponseCode(out, in, 
                "{\"operation\":\"requestLeaderboard\",\"playerName\":\"" + userA + "\"}", 
                "SUCCESS", "requestLeaderboard con utente esistente");

            // Classifica per giocatore inesistente
            assertResponseCode(out, in, 
                "{\"operation\":\"requestLeaderboard\",\"playerName\":\"ghostPlayer_404\"}", 
                "PLAYER_NOT_FOUND", "requestLeaderboard con giocatore inesistente");

            // Statistiche personali utente loggato
            assertResponseCode(out, in, 
                "{\"operation\":\"requestPlayerStats\"}", 
                "SUCCESS", "requestPlayerStats utente autenticato");

            // -------------------------------------------------------------
            // BLOCCO 7: AGGIORNAMENTO CREDENZIALI
            // -------------------------------------------------------------
            System.out.println("\n--- BLOCCO 7: Aggiornamento Credenziali ---");

            // oldPsw errata
            assertResponseCode(out, in, 
                "{\"operation\":\"updateCredentials\",\"oldUsername\":\"" + userA + "\",\"oldPsw\":\"wrongPsw\",\"newPsw\":\"newSecret\"}", 
                "INVALID_CREDENTIALS", "updateCredentials con oldPsw errata");

            // newUsername già registrato (collisione con userB)
            assertResponseCode(out, in, 
                "{\"operation\":\"updateCredentials\",\"oldUsername\":\"" + userA + "\",\"oldPsw\":\"" + psw + "\",\"newUsername\":\"" + userB + "\"}", 
                "USERNAME_ALREADY_TAKEN", "updateCredentials con newUsername già in uso");

            // Aggiornamento regolare della sola password
            assertResponseCode(out, in, 
                "{\"operation\":\"updateCredentials\",\"oldUsername\":\"" + userA + "\",\"oldPsw\":\"" + psw + "\",\"newPsw\":\"newValidPsw456!\"}", 
                "SUCCESS", "updateCredentials regolare (aggiornamento password)");

            // -------------------------------------------------------------
            // BLOCCO 8: LOGOUT E CHIUSURA
            // -------------------------------------------------------------
            System.out.println("\n--- BLOCCO 8: Logout e revoca sessione ---");

            // Logout regolare
            assertResponseCode(out, in, 
                "{\"operation\":\"logout\"}", 
                "SUCCESS", "Logout regolare dell'utente");

            // Verifica post-logout: richiesta che richiede login deve fallire
            assertResponseCode(out, in, 
                "{\"operation\":\"requestPlayerStats\"}", 
                "NOT_LOGGED_IN", "requestPlayerStats post-logout (permesso revocato)");

            System.out.println("\n================================================================");
            System.out.println(" ESITO VERIFICA PROTOCOLLO: ");
            System.out.println(" TEST SUPERATI: " + testsPassed);
            System.out.println(" TEST FALLITI : " + testsFailed);
            System.out.println("================================================================");

        } catch (Exception e) {
            System.err.println("\n[ERRORE FATALE] Connessione interrotta o errore di socket: " + e.getMessage());
        }
    }
}