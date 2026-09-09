package client.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

import common.protocol.request.Request;
import common.protocol.response.ServerResponse;

import java.lang.reflect.Type;

/**
 * Gestisce la connessione TCP sincrona verso il server mediante Java NIO {@link SocketChannel}.
 * <p>
 * Incapsula il protocollo a riga di testo delimitato da terminatore newline ('\n'),
 * occupandosi della serializzazione delle richieste e deserializzazione delle risposte JSON.
 * Non è thread-safe, per questo l'accesso al buffer interno e al canale deve essere sequenziale.
 */
public class ServerConnection implements AutoCloseable {
    private static final boolean DEBUG = false;

    private final SocketChannel socketChannel;
    private final ByteBuffer byteBuffer;
    private static final Gson gson = new Gson();

    /**
     * Apre un canale {@link SocketChannel} e stabilisce la connessione con il server all'indirizzo specificato.
     *
     * @param host hostname o indirizzo IP del server
     * @param port porta TCP di ascolto del server
     * @throws IOException se la connessione fallisce o il canale non può essere aperto
     */
    public ServerConnection(String host, int port) throws IOException {
        this.socketChannel = SocketChannel.open();
        this.socketChannel.connect(new InetSocketAddress(host, port));
        this.byteBuffer = ByteBuffer.allocate(4096);
    }

    /**
     * Serializza la richiesta in formato JSON e la trasmette integralmente sul canale TCP.
     * <p>
     * Metodo con side-effect sul socket sottostante, garantisce lo svuotamento completo
     * del payload sul canale prima di ritornare.
     *
     * @param request oggetto richiesta da trasmettere
     * @throws IOException se si verifica un errore durante la scrittura sul canale TCP
     */
    public void sendRequest(Request request) throws IOException {
        byteBuffer.clear();
        
        String jsonString = gson.toJson(request) + "\n";
        byte[] payloadBytes = jsonString.getBytes(StandardCharsets.UTF_8);

        byteBuffer.put(payloadBytes);
        byteBuffer.flip();

        while (byteBuffer.hasRemaining()) {
            socketChannel.write(byteBuffer);
        }
    }

    /**
     * Rimane in attesa di un messaggio completo delimitato da newline ('\n') sul canale TCP
     * e ne deserializza la risposta JSON.
     * <p>
     * Chiamata bloccante con side-effect sul buffer interno e sullo stream TCP.
     *
     * @param <T> tipo del payload atteso nella risposta
     * @param payloadType tipo generico {@link Type} associato a {@link ServerResponse}
     * @return la risposta del server deserializzata
     * @throws IOException se il server chiude la connessione (EOF) o si verificano errori di I/O
     */
    public <T> ServerResponse<T> receiveResponse(Type payloadType) throws IOException {
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        boolean messageComplete = false;

        byteBuffer.clear();

        while (!messageComplete) {
            int bytesRead = socketChannel.read(byteBuffer);
            
            if (bytesRead == -1) {
                throw new IOException("La connessione TCP è stata chiusa inaspettatamente dal server.");
            }
            
            byteBuffer.flip();
            
            while (byteBuffer.hasRemaining()) {
                byte b = byteBuffer.get();

                if (b == '\n') {
                    messageComplete = true;
                    break;
                } else {
                    byteArray.write(b);
                }
            }
            byteBuffer.compact();
        }
        String jsonResponse = byteArray.toString(StandardCharsets.UTF_8.name());
        if (DEBUG) {
            System.out.println("[DEBUG RICEZIONE] JSON grezzo: " + jsonResponse);
        }
        return gson.fromJson(jsonResponse, payloadType);
    }

    /**
     * Chiude il canale di rete se aperto.
     *
     * @throws IOException se si verifica un errore durante la chiusura del socket
     */
    @Override
    public void close() throws IOException {
        if (this.socketChannel != null && this.socketChannel.isOpen()) {
            this.socketChannel.close();
        }
    }
}