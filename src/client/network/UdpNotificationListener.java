package client.network;

import com.google.gson.Gson;
import common.protocol.response.payload.GameFinishedNotificationPayload;
import common.protocol.response.payload.GameInfoPayload;
import common.protocol.response.payload.GameStatsPayload;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UdpNotificationListener {

    private final DatagramSocket socket;
    private final Gson gson;
    private Thread listenerThread;
    private volatile boolean running;

    public UdpNotificationListener() throws SocketException {
        // Apertura su porta effimera libera assegnata dal SO
        this.socket = new DatagramSocket(0);
        this.gson = new Gson();
        this.running = false;
    }

    public int getLocalPort() {
        return this.socket.getLocalPort();
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        this.running = true;
        this.listenerThread = new Thread(this::listenLoop, "UDP-Notification-Worker");
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }

    public synchronized void stop() {
        this.running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Sblocca socket.receive(...) sollevando SocketException
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    private void listenLoop() {
        byte[] buffer = new byte[4096];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String json = new String(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength(),
                        StandardCharsets.UTF_8
                );

                GameFinishedNotificationPayload payload = gson.fromJson(json, GameFinishedNotificationPayload.class);
                if (payload != null) {
                    displayNotification(payload);
                }

            } catch (SocketException e) {
                // Interruzione normale del socket causata da stop()
                if (!running) {
                    break;
                }
                System.err.println("\n[UDP] Errore di connessione sul socket: " + e.getMessage());
            } catch (IOException e) {
                if (running) {
                    System.err.println("\n[UDP] Errore durante la ricezione del pacchetto: " + e.getMessage());
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("\n[UDP] Errore durante la decodifica del messaggio: " + e.getMessage());
                }
            }
        }
    }

    private void displayNotification(GameFinishedNotificationPayload payload) {
        GameInfoPayload info = payload.getGameInfo();
        GameStatsPayload stats = payload.getGameStats();

        System.out.println("\n\n=======================================================");
        System.out.println("  [NOTIFICA ASINCRONA] Partita #" + payload.getGameId() + " conclusa!");
        System.out.println("=======================================================");
        
        if (info != null && info.getFinalAllocation() != null) {
            System.out.println("Soluzioni corrette dei gruppi:");
            int gIdx = 1;
            for (List<String> group : info.getFinalAllocation()) {
                System.out.println("  Gruppo " + gIdx++ + ": " + String.join(", ", group));
            }
            System.out.println("I tuoi risultati nel round -> Errori: " + info.getErrors() + ", Punteggio: " + info.getScore());
        }

        if (stats != null) {
            System.out.println("Statistiche globali:");
            System.out.println("  Partecipanti: " + stats.getTotalParticipants()
                    + " | Completati: " + stats.getParticipantsFinished()
                    + " | Vincitori: " + stats.getParticipantsWon()
                    + " | Media punteggio: " + String.format("%.2f", stats.getAverageScore()));
        }
        System.out.println("=======================================================");
        System.out.print("\nSeleziona un'opzione: ");
        System.out.flush();
    }
}