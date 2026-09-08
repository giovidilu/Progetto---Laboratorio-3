package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class TestConcurrentLoginMain {

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 8888;

        String testUser = "concurrentUser_" + System.currentTimeMillis();
        String testPsw = "PswConcorrente123!";

        System.out.println("=== TEST CONCORRENZA: DOPPIO LOGIN SIMULTANEO ===");

        // 1. Registrazione preventiva dell'utente di test
        try (Socket regSocket = new Socket(host, port);
             PrintWriter regOut = new PrintWriter(new OutputStreamWriter(regSocket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader regIn = new BufferedReader(new InputStreamReader(regSocket.getInputStream(), StandardCharsets.UTF_8))) {
            
            regOut.println("{\"operation\":\"register\",\"username\":\"" + testUser + "\",\"psw\":\"" + testPsw + "\"}");
            String regRes = regIn.readLine();
            System.out.println("Registrazione utente di test: " + regRes);
        }

        // 2. Apertura di due connessioni socket distinte
        Socket s1 = new Socket(host, port);
        PrintWriter out1 = new PrintWriter(new OutputStreamWriter(s1.getOutputStream(), StandardCharsets.UTF_8), true);
        BufferedReader in1 = new BufferedReader(new InputStreamReader(s1.getInputStream(), StandardCharsets.UTF_8));

        Socket s2 = new Socket(host, port);
        PrintWriter out2 = new PrintWriter(new OutputStreamWriter(s2.getOutputStream(), StandardCharsets.UTF_8), true);
        BufferedReader in2 = new BufferedReader(new InputStreamReader(s2.getInputStream(), StandardCharsets.UTF_8));

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(2);

        AtomicReference<String> res1 = new AtomicReference<>();
        AtomicReference<String> res2 = new AtomicReference<>();

        String loginPayload1 = "{\"operation\":\"login\",\"username\":\"" + testUser + "\",\"psw\":\"" + testPsw + "\",\"udpPort\":8881}";
        String loginPayload2 = "{\"operation\":\"login\",\"username\":\"" + testUser + "\",\"psw\":\"" + testPsw + "\",\"udpPort\":8882}";

        // Thread Client 1
        new Thread(() -> {
            try {
                startGate.await(); // Attende il via simultaneo
                out1.println(loginPayload1);
                res1.set(in1.readLine());
            } catch (Exception e) {
                res1.set("EXCEPTION: " + e.getMessage());
            } finally {
                endGate.countDown();
            }
        }).start();

        // Thread Client 2
        new Thread(() -> {
            try {
                startGate.await(); // Attende il via simultaneo
                out2.println(loginPayload2);
                res2.set(in2.readLine());
            } catch (Exception e) {
                res2.set("EXCEPTION: " + e.getMessage());
            } finally {
                endGate.countDown();
            }
        }).start();

        // 3. Rilascio sincronizzato dei due thread
        startGate.countDown();
        endGate.await();

        System.out.println("\nRisposta Connessione 1: " + res1.get());
        System.out.println("Risposta Connessione 2: " + res2.get());

        // 4. Verifica esiti differenziati: uno SUCCESS, uno BAD_REQUEST
        boolean hasSuccess = (res1.get().contains("\"status\":\"SUCCESS\"") || res2.get().contains("\"status\":\"SUCCESS\""));
        boolean hasBadRequest = (res1.get().contains("\"status\":\"BAD_REQUEST\"") || res2.get().contains("\"status\":\"BAD_REQUEST\""));

        if (hasSuccess && hasBadRequest) {
            System.out.println("\n[PASS] Mutua esclusione rispettata: un solo login accettato, l'altro respinto.");
        } else {
            System.err.println("\n[FAIL] Comportamento anomalo nel login concorrente.");
        }

        // 5. Verifica che il client vincente rimanga pienamente operativo
        PrintWriter winnerOut = res1.get().contains("\"status\":\"SUCCESS\"") ? out1 : out2;
        BufferedReader winnerIn = res1.get().contains("\"status\":\"SUCCESS\"") ? in1 : in2;

        System.out.println("\nVerifica operatività client autenticato (invio requestPlayerStats)...");
        winnerOut.println("{\"operation\":\"requestPlayerStats\"}");
        String statsRes = winnerIn.readLine();
        System.out.println("<- Risposta client vincente: " + statsRes);

        if (statsRes != null && statsRes.contains("\"status\":\"SUCCESS\"")) {
            System.out.println("[PASS] La sessione del client vincente e' rimasta attiva e valida.");
        } else {
            System.err.println("[FAIL] La sessione del client vincente e' risultata compromessa.");
        }

        // 6. Cleanup e logout
        winnerOut.println("{\"operation\":\"logout\"}");
        winnerIn.readLine();
        s1.close();
        s2.close();
    }
}