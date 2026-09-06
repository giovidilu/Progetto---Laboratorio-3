package server.service;

import com.google.gson.Gson;
import common.model.GameTemplate;
import common.model.ProposalResult;
import common.model.User;
import common.model.WordGroup;
import common.protocol.response.payload.GameFinishedNotificationPayload;
import server.repository.GameRepository;
import server.repository.UserRepository;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestUdpNotificationMain {

    public static void main(String[] args) {
        System.out.println("=== Avvio Test Integrazione Notifiche Asincrone UDP ===");

        DatagramSocket fakeClientSocket = null;
        UdpNotifier udpNotifier = null;
        GameManager gameManager = null;

        try {
            // 1. Socket client fittizio su porta effimera
            fakeClientSocket = new DatagramSocket(0);
            fakeClientSocket.setSoTimeout(3000);
            int clientPort = fakeClientSocket.getLocalPort();
            InetSocketAddress clientEndpoint = new InetSocketAddress(InetAddress.getLoopbackAddress(), clientPort);

            // 2. Registrazione utente in UserRepository e login in SessionManager
            File tempUserFile = File.createTempFile("test_udp_users", ".json");
            tempUserFile.deleteOnExit();
            UserRepository userRepo = new UserRepository(tempUserFile.getAbsolutePath());
            
            User alice = new User("alice", "dummy_hash", "dummy_salt");
            userRepo.addUser(alice);

            SessionManager sessionManager = new SessionManager();
            sessionManager.login("alice", clientEndpoint);

            // 3. Inizializzazione GameRepository temporaneo
            File tempGameDir = new File(System.getProperty("java.io.tmpdir"), "test_udp_games_" + System.currentTimeMillis());
            tempGameDir.mkdirs();
            tempGameDir.deleteOnExit();
            GameRepository gameRepo = new GameRepository(tempGameDir.getAbsolutePath());

            // 4. Configurazione dei gruppi e GameTemplate (gameId, groups)
            List<WordGroup> groups = Arrays.asList(
                new WordGroup("ANIMALS", Arrays.asList("CAT", "DOG", "BEAR", "LION")),
                new WordGroup("COLORS", Arrays.asList("RED", "BLUE", "GREEN", "YELLOW")),
                new WordGroup("CITIES", Arrays.asList("ROME", "PARIS", "BERLIN", "MADRID")),
                new WordGroup("FRUITS", Arrays.asList("APPLE", "BANANA", "ORANGE", "PEAR"))
            );

            Map<Integer, GameTemplate> templates = new HashMap<>();
            templates.put(0, new GameTemplate(0, groups));

            // 5. Inizializzazione di UdpNotifier e GameManager
            int serverUdpPort = 9999;
            udpNotifier = new UdpNotifier(serverUdpPort);
            gameManager = new GameManager(templates, gameRepo, userRepo, sessionManager, udpNotifier, 600000L);

            int roundIdBefore = gameManager.getCurrentGameId();

            // 6. Proposta di alice usando il getter reale getMoveOutcome()
            ProposalResult propResult = gameManager.submitProposal("alice", Arrays.asList("CAT", "DOG", "BEAR", "LION"));
            System.out.println("Mossa inviata con esito: " + propResult.getMoveOutcome());

            // 7. Scadenza/rotazione della partita
            System.out.println("Invocazione rotateGame()...");
            gameManager.rotateGame();

            // 8. Ricezione pacchetto UDP
            byte[] recvBuffer = new byte[4096];
            DatagramPacket receivedPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
            fakeClientSocket.receive(receivedPacket);

            String jsonReceived = new String(
                receivedPacket.getData(),
                receivedPacket.getOffset(),
                receivedPacket.getLength(),
                StandardCharsets.UTF_8
            );

            System.out.println("[UDP RICEVUTO] " + jsonReceived);

            // 9. Validazione del payload deserializzato
            Gson gson = new Gson();
            GameFinishedNotificationPayload payload = gson.fromJson(jsonReceived, GameFinishedNotificationPayload.class);

            if (payload == null) {
                throw new AssertionError("Payload deserializzato nullo.");
            }
            if (payload.getGameId() != roundIdBefore) {
                throw new AssertionError("GameId errato: atteso " + roundIdBefore + ", ricevuto " + payload.getGameId());
            }
            if (payload.getGameInfo() == null || payload.getGameInfo().getFinalAllocation() == null) {
                throw new AssertionError("La notifica non ha svelato la soluzione corretta delle parole!");
            }
            if (payload.getGameStats() == null || payload.getGameStats().getTotalParticipants() != 1) {
                throw new AssertionError("Statistiche aggregate errate o mancanti.");
            }

            System.out.println("Soluzioni rivelate: " + payload.getGameInfo().getFinalAllocation().size() + " gruppi.");
            System.out.println("Partecipanti registrati: " + payload.getGameStats().getTotalParticipants());
            System.out.println("=== Test Integrazione Notifiche Asincrone UDP COMPLETATO CON SUCCESSO ===");

        } catch (Exception e) {
            System.err.println("TEST FALLITO: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (fakeClientSocket != null && !fakeClientSocket.isClosed()) {
                fakeClientSocket.close();
            }
            if (udpNotifier != null) {
                udpNotifier.close();
            }
        }
    }
}