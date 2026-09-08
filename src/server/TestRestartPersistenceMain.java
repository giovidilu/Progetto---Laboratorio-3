package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestRestartPersistenceMain {

    private static String send(PrintWriter out, BufferedReader in, String json) throws Exception {
        out.println(json);
        return in.readLine();
    }

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 8888;

        System.out.println("=== TEST VERIFICA DATI DOPO IL RIAVVIO DEL SERVER ===");

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            String userEsistente = "winTester_1788875212752";
            String pswEsistente = "PswWin123!";
                        
            String loginRes = send(out, in, "{\"operation\":\"login\",\"username\":\"" 
                + userEsistente + "\",\"psw\":\"" + pswEsistente + "\",\"udpPort\":8899}");
            System.out.println("Login utente storico: " + loginRes);
            System.out.println("Login utente storico: " + loginRes);

            // 2. Verifica lettura statistiche storiche personali
            String statsRes = send(out, in, "{\"operation\":\"requestPlayerStats\"}");
            System.out.println("Statistiche personali dopo il riavvio: " + statsRes);

            // 3. Verifica consultazione di una partita storica archiviata su disco (es. gameId 1)
            String histRes = send(out, in, "{\"operation\":\"requestGameInfo\",\"gameId\":1}");
            System.out.println("Partita storica ID 1: " + histRes);

            // 4. Verifica stato partita attiva del server riavviato
            String activeRes = send(out, in, "{\"operation\":\"requestGameInfo\"}");
            System.out.println("Stato partita attiva post-riavvio: " + activeRes);

            boolean passLogin = loginRes.contains("\"status\":\"SUCCESS\"");
            boolean passHist = histRes.contains("\"status\":\"SUCCESS\"") && histRes.contains("\"state\":\"FINISHED\"");
            boolean passActive = activeRes.contains("\"status\":\"SUCCESS\"") && activeRes.contains("\"state\":\"ONGOING\"") && activeRes.contains("\"errors\":0");

            if (passLogin && passHist && passActive) {
                System.out.println("\n[PASS] Dati ricaricati correttamente e nuova partita attiva avviata pulita.");
            } else {
                System.err.println("\n[FAIL] Problemi nel recupero dello storico o nello stato iniziale post-riavvio.");
            }

            send(out, in, "{\"operation\":\"logout\"}");
        }
    }
}