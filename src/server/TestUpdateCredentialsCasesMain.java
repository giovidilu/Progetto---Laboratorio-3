package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestUpdateCredentialsCasesMain {

    private static String send(PrintWriter out, BufferedReader in, String json) throws Exception {
        out.println(json);
        return in.readLine();
    }

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 8888;

        String u0 = "updUser_" + System.currentTimeMillis();
        String p0 = "PswIniziale1!";

        System.out.println("=== TEST UPDATE CREDENTIALS: 3 CASI DISTINTI ===");

        try (Socket s = new Socket(host, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {

            // 0. Registrazione e Login iniziale
            send(out, in, "{\"operation\":\"register\",\"username\":\"" + u0 + "\",\"psw\":\"" + p0 + "\"}");
            send(out, in, "{\"operation\":\"login\",\"username\":\"" + u0 + "\",\"psw\":\"" + p0 + "\",\"udpPort\":8891}");

            // CASO 1: Solo nuova password
            String p1 = "PswModificata2!";
            System.out.println("\n[CASO 1] Aggiornamento sola password...");
            String res1 = send(out, in, "{\"operation\":\"updateCredentials\",\"oldUsername\":\"" + u0 + "\",\"oldPsw\":\"" + p0 + "\",\"newPsw\":\"" + p1 + "\"}");
            System.out.println("<- Risposta: " + res1);
            boolean pass1 = res1.contains("\"status\":\"SUCCESS\"");

            // CASO 2: Solo nuovo username
            String u1 = u0 + "_renamed";
            System.out.println("\n[CASO 2] Aggiornamento solo username...");
            String res2 = send(out, in, "{\"operation\":\"updateCredentials\",\"oldUsername\":\"" + u0 + "\",\"newUsername\":\"" + u1 + "\",\"oldPsw\":\"" + p1 + "\"}");
            System.out.println("<- Risposta: " + res2);
            boolean pass2 = res2.contains("\"status\":\"SUCCESS\"");

            // CASO 3: Entrambi insieme (nuovo username + nuova password)
            String u2 = u1 + "_final";
            String p2 = "PswFinale3!";
            System.out.println("\n[CASO 3] Aggiornamento simultaneo username e password...");
            String res3 = send(out, in, "{\"operation\":\"updateCredentials\",\"oldUsername\":\"" + u1 + "\",\"newUsername\":\"" + u2 + "\",\"oldPsw\":\"" + p1 + "\",\"newPsw\":\"" + p2 + "\"}");
            System.out.println("<- Risposta: " + res3);
            boolean pass3 = res3.contains("\"status\":\"SUCCESS\"");

            // Logout dal client primario
            send(out, in, "{\"operation\":\"logout\"}");

            // Verifica incrociata con una nuova socket: login con le credenziali finali
            System.out.println("\n[VERIFICA FINALE] Tentativo login con credenziali finali (u2, p2)...");
            try (Socket verifySocket = new Socket(host, port);
                 PrintWriter vOut = new PrintWriter(new OutputStreamWriter(verifySocket.getOutputStream(), StandardCharsets.UTF_8), true);
                 BufferedReader vIn = new BufferedReader(new InputStreamReader(verifySocket.getInputStream(), StandardCharsets.UTF_8))) {

                String vRes = send(vOut, vIn, "{\"operation\":\"login\",\"username\":\"" + u2 + "\",\"psw\":\"" + p2 + "\",\"udpPort\":8892}");
                System.out.println("<- Risposta login finale: " + vRes);
                boolean passVerify = vRes.contains("\"status\":\"SUCCESS\"");

                send(vOut, vIn, "{\"operation\":\"logout\"}");

                if (pass1 && pass2 && pass3 && passVerify) {
                    System.out.println("\n[PASS] Tutti e 3 i casi di updateCredentials hanno avuto successo e le credenziali finali sono attive.");
                } else {
                    System.err.println("\n[FAIL] Uno o piu' casi di aggiornamento hanno fallito.");
                }
            }
        }
    }
}