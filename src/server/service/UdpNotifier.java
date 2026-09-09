package server.service;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Trasmettitore asincrono di notifiche UDP verso gli endpoint dei client registrati.
 * <p>
 * Incapsula un {@link DatagramSocket} di trasmissione server e provvede all'invio dei datagrammi JSON.
 * Thread-safe: le operazioni di invio su {@link DatagramSocket} in Java sono thread-safe a livello di runtime.
 */
public class UdpNotifier implements Closeable {

    private final DatagramSocket datagramSocket;

    /**
     * Alloca il socket UDP vincolandolo alla porta specificata.
     *
     * @param serverUdpPort porta UDP locale di ascolto/trasmissione
     * @throws SocketException se non è possibile effettuare il bind del socket
     */
    public UdpNotifier(int serverUdpPort) throws SocketException {
        this.datagramSocket = new DatagramSocket(serverUdpPort);
    }

    /**
     * Invia un messaggio testuale codificato in UTF-8 all'indirizzo e porta remoti indicati.
     * <p>
     * Metodo con effetto collaterale di rete. Non lancia eccezioni se i parametri risultano nulli.
     *
     * @param targetEndpoint destinazione remota IP e porta del client
     * @param jsonMessage stringa JSON formattata da trasmettere
     * @throws IOException se si verificano errori di I/O nell'invio del pacchetto
     */
    public void sendNotification(InetSocketAddress targetEndpoint, String jsonMessage) throws IOException {
        if (targetEndpoint == null || jsonMessage == null) {
            return;
        }
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, targetEndpoint);
        this.datagramSocket.send(packet);
    }

    /**
     * Rilascia il socket UDP sottostante.
     */
    @Override
    public void close() {
        if (this.datagramSocket != null && !this.datagramSocket.isClosed()) {
            this.datagramSocket.close();
        }
    }
}