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

/**
 * Ricevitore asincrono di notifiche UDP trasmesse dal server al termine delle partite.
 * <p>
 * Apre un {@link DatagramSocket} su porta effimera locale ed esegue un thread daemon
 * dedicato all'ascolto e deserializzazione dei pacchetti di notifica JSON ricevuti.
 * I metodi di controllo del ciclo di vita ({@code start}, {@code stop}) sono thread-safe.
 */
public class UdpNotificationListener {

    private final DatagramSocket socket;
    private final Gson gson;
    private Thread listenerThread;
    private volatile boolean running;

    /**
     * Crea il listener allocando un {@link DatagramSocket} su una porta effimera libera.
     *
     * @throws SocketException se non è possibile allocare il socket UDP
     */
    public UdpNotificationListener() throws SocketException {
        this.socket = new DatagramSocket(0);
        this.gson = new Gson();
        this.running = false;
    }

    public int getLocalPort() {
        return this.socket.getLocalPort();
    }

    /**
     * Avvia il thread daemon di ascolto dei pacchetti UDP se non è già in esecuzione.
     * <p>
     * Metodo thread-safe con side-effect sull'avvio del thread di background.
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        this.running = true;
        this.listenerThread = new Thread(this::listenLoop, "UDP-Notification-Worker");
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }

    /**
     * Arresta il loop di ricezione e rilascia il socket UDP sottostante.
     * <p>
     * Metodo thread-safe: chiude il socket forzando lo sblocco di eventuali letture bloccanti.
     */
    public synchronized void stop() {
        this.running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    /**
     * Loop eseguito dal thread daemon per ricevere ed elaborare i datagrammi in arrivo.
     */
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
        System.out.println("=======================================================\n");
        System.out.flush();
    }
}