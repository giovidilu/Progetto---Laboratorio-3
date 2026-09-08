package server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TestMistakesLimitMain {

    private static String send(PrintWriter out, BufferedReader in, String json) throws Exception {
        out.println(json);
        return in.readLine();
    }

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 8888;
        Gson gson = new Gson();

        String testUser = "mistakeTester_" + System.currentTimeMillis();
        String testPsw = "PswTest123!";

        System.out.println("=== TEST LIMITE 4 ERRORI E 5a PROPOSTA (ALREADY_COMPLETED) ===");

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // 1. Registrazione e Login
            send(out, in, "{\"operation\":\"register\",\"username\":\"" + testUser + "\",\"psw\":\"" + testPsw + "\"}");
            send(out, in, "{\"operation\":\"login\",\"username\":\"" + testUser + "\",\"psw\":\"" + testPsw + "\",\"udpPort\":8895}");

            // 2. Lettura delle 16 parole della partita attiva
            String infoRes = send(out, in, "{\"operation\":\"requestGameInfo\"}");
            JsonObject infoJson = JsonParser.parseString(infoRes).getAsJsonObject();
            JsonArray wordsArray = infoJson.getAsJsonObject("payload").getAsJsonArray("words");

            List<String> words = new ArrayList<>();
            for (JsonElement elem : wordsArray) {
                words.add(elem.getAsString());
            }
            System.out.println("Parole attive caricate: " + words.size());

            // 3. Invio di 4 proposte errate
            // Fissiamo words[0], words[1], words[2] e variamo il quarto elemento con words[3..6]
            for (int mistakeNum = 1; mistakeNum <= 4; mistakeNum++) {
                List<String> proposal = List.of(
                    words.get(0),
                    words.get(1),
                    words.get(2),
                    words.get(2 + mistakeNum)
                );

                JsonObject reqObj = new JsonObject();
                reqObj.addProperty("operation", "submitProposal");
                reqObj.add("words", gson.toJsonTree(proposal));

                String res = send(out, in, reqObj.toString());
                System.out.println("Tentativo " + mistakeNum + " -> Risposta: " + res);
            }

            // 4. Invio della 5ª proposta: deve essere rifiutata con BAD_REQUEST (ALREADY_COMPLETED)
            List<String> extraProposal = List.of(words.get(0), words.get(1), words.get(2), words.get(7));
            JsonObject extraReq = new JsonObject();
            extraReq.addProperty("operation", "submitProposal");
            extraReq.add("words", gson.toJsonTree(extraProposal));

            String extraRes = send(out, in, extraReq.toString());
            System.out.println("\nTentativo 5 (dopo limite) -> Risposta: " + extraRes);

            // 5. Verifica stato partita per l'utente: deve avere 4 errori e punteggio penalizzato (-16)
            String finalInfoRes = send(out, in, "{\"operation\":\"requestGameInfo\"}");
            System.out.println("Stato finale utente: " + finalInfoRes);

            boolean passLimit = extraRes.contains("\"status\":\"BAD_REQUEST\"");
            boolean passState = finalInfoRes.contains("\"errors\":4") && finalInfoRes.contains("\"score\":-16");

            if (passLimit && passState) {
                System.out.println("\n[PASS] 4 errori portano a conclusione partita e la 5a proposta e' respinta regolarmente.");
            } else {
                System.err.println("\n[FAIL] Comportamento anomalo sul limite errori o sul punteggio penalita'.");
            }

            send(out, in, "{\"operation\":\"logout\"}");
        }
    }
}