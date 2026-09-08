package server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TestThreeCorrectWinsMain {

    private static String send(PrintWriter out, BufferedReader in, String json) throws Exception {
        out.println(json);
        return in.readLine();
    }

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 8888;
        Gson gson = new Gson();

        String testUser = "winTester_" + System.currentTimeMillis();
        String testPsw = "PswWin123!";

        System.out.println("=== TEST TRE GRUPPI CORRETTI -> WON (4o IMPLICITO) ===");

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // 1. Registrazione e Login
            send(out, in, "{\"operation\":\"register\",\"username\":\"" + testUser + "\",\"psw\":\"" + testPsw + "\"}");
            send(out, in, "{\"operation\":\"login\",\"username\":\"" + testUser + "\",\"psw\":\"" + testPsw + "\",\"udpPort\":8896}");

            // 2. Lettura dello stato della partita attiva per ricavare l'ID partita corrente tramite submit di test o requestGameInfo
            // Inviamo un submitProposal fittizio per leggere l'ID esatto della partita attiva dal payload
            String probeRes = send(out, in, "{\"operation\":\"submitProposal\",\"words\":[\"W1\",\"W2\"]}");
            JsonObject probeJson = JsonParser.parseString(probeRes).getAsJsonObject();
            
            // In caso di MALFORMED_PROPOSAL recuperiamo il gameId interrogando requestLeaderboard o leggendo da games
            // Alternativamente leggiamo i template da Connections_Data.json
            JsonArray allTemplates;
            try (FileReader fr = new FileReader("data/Connections_Data.json", StandardCharsets.UTF_8)) {
                allTemplates = JsonParser.parseReader(fr).getAsJsonArray();
            }

            // Recupero delle 16 parole attive per trovare il template corrispondente nel dataset
            String infoRes = send(out, in, "{\"operation\":\"requestGameInfo\"}");
            JsonObject infoJson = JsonParser.parseString(infoRes).getAsJsonObject();
            JsonArray currentWordsArray = infoJson.getAsJsonObject("payload").getAsJsonArray("words");

            List<String> activeWords = new ArrayList<>();
            for (JsonElement e : currentWordsArray) {
                activeWords.add(e.getAsString().toUpperCase());
            }

            // Ricerca del template corrispondente nel dataset
            JsonObject matchedTemplate = null;
            for (JsonElement tElem : allTemplates) {
                JsonObject tObj = tElem.getAsJsonObject();
                JsonArray groups = tObj.getAsJsonArray("groups");
                List<String> templateWords = new ArrayList<>();
                for (JsonElement gElem : groups) {
                    for (JsonElement wElem : gElem.getAsJsonObject().getAsJsonArray("words")) {
                        templateWords.add(wElem.getAsString().toUpperCase());
                    }
                }
                if (activeWords.containsAll(templateWords) && templateWords.containsAll(activeWords)) {
                    matchedTemplate = tObj;
                    break;
                }
            }

            if (matchedTemplate == null) {
                System.err.println("[ERRORE] Impossibile trovare il template della partita attiva in Connections_Data.json");
                return;
            }

            JsonArray groups = matchedTemplate.getAsJsonArray("groups");
            System.out.println("Template identificato. Gruppi totali: " + groups.size());

            // Estrazione delle quadruple dei 4 gruppi
            List<List<String>> correctGroups = new ArrayList<>();
            for (JsonElement gElem : groups) {
                List<String> gWords = new ArrayList<>();
                for (JsonElement wElem : gElem.getAsJsonObject().getAsJsonArray("words")) {
                    gWords.add(wElem.getAsString());
                }
                correctGroups.add(gWords);
            }

            // 3. Invio Gruppo 1 (Corretto)
            JsonObject req1 = new JsonObject();
            req1.addProperty("operation", "submitProposal");
            req1.add("words", gson.toJsonTree(correctGroups.get(0)));
            String res1 = send(out, in, req1.toString());
            System.out.println("\nGruppo 1 -> Risposta: " + res1);
            boolean pass1 = res1.contains("\"moveOutcome\":\"CORRECT\"") && !res1.contains("\"gameOutcome\":\"WON\"");

            // 4. Invio Gruppo 2 (Corretto)
            JsonObject req2 = new JsonObject();
            req2.addProperty("operation", "submitProposal");
            req2.add("words", gson.toJsonTree(correctGroups.get(1)));
            String res2 = send(out, in, req2.toString());
            System.out.println("Gruppo 2 -> Risposta: " + res2);
            boolean pass2 = res2.contains("\"moveOutcome\":\"CORRECT\"") && !res2.contains("\"gameOutcome\":\"WON\"");

            // 5. Invio Gruppo 3 (Corretto) -> DEVE SCATTARE LA VITTORIA (gameOutcome = WON)
            JsonObject req3 = new JsonObject();
            req3.addProperty("operation", "submitProposal");
            req3.add("words", gson.toJsonTree(correctGroups.get(2)));
            String res3 = send(out, in, req3.toString());
            System.out.println("Gruppo 3 -> Risposta: " + res3);
            boolean pass3 = res3.contains("\"moveOutcome\":\"CORRECT\"") && res3.contains("\"gameOutcome\":\"WON\"");

            // 6. Invio del 4° gruppo (dovrebbe essere respinto con BAD_REQUEST perché già conclusa)
            JsonObject req4 = new JsonObject();
            req4.addProperty("operation", "submitProposal");
            req4.add("words", gson.toJsonTree(correctGroups.get(3)));
            String res4 = send(out, in, req4.toString());
            System.out.println("Gruppo 4 (post-vittoria) -> Risposta: " + res4);
            boolean pass4 = res4.contains("\"status\":\"BAD_REQUEST\"");

            // 7. Verifica finale punteggio e stato da requestGameInfo
            String finalInfo = send(out, in, "{\"operation\":\"requestGameInfo\"}");
            System.out.println("Stato finale info: " + finalInfo);
            boolean passScore = finalInfo.contains("\"score\":18") && finalInfo.contains("\"errors\":0");

            if (pass1 && pass2 && pass3 && pass4 && passScore) {
                System.out.println("\n[PASS] Vittoria al 3o gruppo conclamata (+18 punti) e 4o invio respinto regolarmente.");
            } else {
                System.err.println("\n[FAIL] Comportamento anomalo sul completamento per 3 gruppi o calcolo punteggio.");
            }

            send(out, in, "{\"operation\":\"logout\"}");
        }
    }
}