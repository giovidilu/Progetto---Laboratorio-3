package client.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Gestore immutabile della configurazione del client.
 * <p>
 * Carica e valida all'istanziazione i parametri di rete dal file di proprietà.
 * È una classe thread-safe perchè, dopo la costruzione, lo stato è costituito
 * da campi in sola lettura.
 */
public class ClientConfig {
    private String serverHost;
    private int serverPort;

    /**
     * Carica i parametri di configurazione dal percorso specificato.
     *
     * @param configFilePath percorso del file .properties da caricare
     * @throws IOException se si verificano errori nell'apertura o lettura del file
     * @throws IllegalArgumentException se il file non contiene la porta o se il valore non è numerico
     */
    public ClientConfig(String configFilePath) throws IOException {
        
        Properties properties = new Properties();
        
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            
            properties.load(fis);
            
            this.serverHost = properties.getProperty("server.host", "localhost");
            
            String portString = properties.getProperty("server.port");
            if (portString != null) {
                this.serverPort = Integer.parseInt(portString);
            } else {
                throw new IllegalArgumentException("La chiave 'server.port' è assente nel file di configurazione.");
            }
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La porta specificata nel file di configurazione non è un numero valido.", e);
        }
    }

    /** Restituisce l'indirizzo host del server. */
    public String getServerHost(){
        return serverHost;
    }

    /** Restituisce la porta TCP del server. */
    public int getServerPort(){
        return serverPort;
    }
}