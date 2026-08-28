package client.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ClientConfig {
    private String serverHost;
    private int serverPort;

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

    public String getServerHost(){
        return serverHost;
    }

    public int getServerPort(){
        return serverPort;
    }
}
