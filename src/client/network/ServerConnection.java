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

public class ServerConnection implements AutoCloseable {
    private final SocketChannel socketChannel;
    private final ByteBuffer byteBuffer;
    private static final Gson gson = new Gson();

    public ServerConnection(String host, int port) throws IOException{

        this.socketChannel = SocketChannel.open();
        this.socketChannel.connect(new InetSocketAddress(host, port));
        this.byteBuffer = ByteBuffer.allocate(4096);
    }

    public void sendRequest(Request request) throws IOException{

        byteBuffer.clear();
        
        // Serializzazione payload e aggiunta delimitatore
        String jsonString = gson.toJson(request) + "\n";

        byte[] payloadBytes = jsonString.getBytes(StandardCharsets.UTF_8);

        byteBuffer.put(payloadBytes);

        // Transazione del buffer alla lettura da parte del canale
        byteBuffer.flip();

        // Trasmissione di tutti i byte
        while (byteBuffer.hasRemaining()) {
            socketChannel.write(byteBuffer);
        }
    }

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
        System.out.println("[DEBUG RICEZIONE] JSON grezzo: " + jsonResponse);
        return gson.fromJson(jsonResponse, payloadType);
    }

    @Override
    public void close() throws IOException{
        if(this.socketChannel != null && this.socketChannel.isOpen()){
            this.socketChannel.close();
        }
    }
    
}
