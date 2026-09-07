package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestProtocolMain {

    private static void sendAndVerify(PrintWriter out, BufferedReader in, String rawJson, String description) throws Exception {
        System.out.println("\n--- TEST: " + description + " ---");
        System.out.println("-> Invio: " + rawJson);
        
        out.println(rawJson);
        out.flush();
        
        String response = in.readLine();
        System.out.println("<- Risposta: " + response);
    }

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8888; // Impostare la porta TCP del proprio server

        System.out.println("Avvio verifica di conformità protocollo Sezione 5...");

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                
                // Username univoco per ogni esecuzione del test
                String testUser = "user_" + System.currentTimeMillis();
                String testPsw = "segreto123";

                // 1. Registrazione nuovo utente -> SUCCESS
                sendAndVerify(out, in, "{\"operation\":\"register\",\"username\":\"" + testUser + "\",\"psw\":\"" + testPsw + "\"}", "Registrazione nuovo");

                // 2. Registrazione duplicata -> USERNAME_ALREADY_TAKEN
                sendAndVerify(out, in, "{\"operation\":\"register\",\"username\":\"" + testUser + "\",\"psw\":\"" + testPsw + "\"}", "Registrazione duplicata");

                // 3. Login utente -> SUCCESS
                sendAndVerify(out, in, "{\"operation\":\"login\",\"username\":\"" + testUser + "\",\"psw\":\"" + testPsw + "\",\"udpPort\":8889}", "Login");

                // 4. Proposta malformata -> MALFORMED_PROPOSAL
                sendAndVerify(out, in, "{\"operation\":\"submitProposal\",\"words\":[\"W1\",\"W2\"]}", "Proposta malformata");

                // 5. Richiesta stato partita corrente (gameId = 0) -> SUCCESS con payload
                sendAndVerify(out, in, "{\"operation\":\"requestGameInfo\",\"gameId\":0}", "Info partita corrente");

                // 6. Statistiche partita corrente (gameId = 0) -> SUCCESS con payload
                sendAndVerify(out, in, "{\"operation\":\"requestGameStats\",\"gameId\":0}", "Stats partita corrente");

                // 7. Classifica generale (topPlayers = 0) -> SUCCESS con payload
                sendAndVerify(out, in, "{\"operation\":\"requestLeaderboard\",\"topPlayers\":0}", "Classifica generale");

                // 8. Statistiche personali -> SUCCESS con payload
                sendAndVerify(out, in, "{\"operation\":\"requestPlayerStats\"}", "Stats personali");

                // 9. Aggiornamento credenziali -> SUCCESS
                sendAndVerify(out, in, "{\"operation\":\"updateCredentials\",\"oldUsername\":\"" + testUser + "\",\"oldPsw\":\"" + testPsw + "\",\"newPsw\":\"nuovaPsw456\"}", "Aggiornamento credenziali");

                // 10. Logout utente -> SUCCESS (ora eseguito al termine delle operazioni)
                sendAndVerify(out, in, "{\"operation\":\"logout\"}", "Logout finale");
            
            System.out.println("\nVerifica completata con successo.");

        } catch (Exception e) {
            System.err.println("Errore durante l'esecuzione del test: " + e.getMessage());
        }
    }
}