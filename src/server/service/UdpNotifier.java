package server.service;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class UdpNotifier implements Closeable {

    private final DatagramSocket datagramSocket;

    public UdpNotifier(int serverUdpPort) throws SocketException {
        this.datagramSocket = new DatagramSocket(serverUdpPort);
    }

    /**
     * Invia un messaggio UDP testuale a uno specifico endpoint remoto.
     *
     * @param targetEndpoint Indirizzo IP e porta UDP di destinazione.
     * @param jsonMessage    Stringa JSON da trasmettere.
     * @throws IOException Se si verifica un errore durante l'invio del datagramma.
     */
    public void sendNotification(InetSocketAddress targetEndpoint, String jsonMessage) throws IOException {
        if (targetEndpoint == null || jsonMessage == null) {
            return;
        }
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, targetEndpoint);
        this.datagramSocket.send(packet);
    }

    @Override
    public void close() {
        if (this.datagramSocket != null && !this.datagramSocket.isClosed()) {
            this.datagramSocket.close();
        }
    }
}