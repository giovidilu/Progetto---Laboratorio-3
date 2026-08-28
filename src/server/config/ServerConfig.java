package server.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class ServerConfig {
    private final String serverHost;
    private final int tcpPort;
    private final int udpPort;

    private final String userDbPath;
    private final String gameDbPath;
    private final String wordsFilePath;

    private final long flushInterval;
    private final long gameDurationMinutes;

    public ServerConfig(String configFilePath) throws IOException {
        if (!Files.exists(Paths.get(configFilePath))) {
            throw new IOException("File di configurazione non trovato: " + configFilePath);
        }

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            properties.load(fis);
        }

        try {
            this.serverHost = properties.getProperty("server.host", "localhost");

            String tcpPortStr = properties.getProperty("server.tcp.port");
            if (tcpPortStr == null || tcpPortStr.isBlank()) {
                throw new IllegalArgumentException("La chiave 'server.tcp.port' è obbligatoria.");
            }
            this.tcpPort = Integer.parseInt(tcpPortStr.trim());

            String udpPortStr = properties.getProperty("server.udp.port");
            if (udpPortStr == null || udpPortStr.isBlank()) {
                throw new IllegalArgumentException("La chiave 'server.udp.port' è obbligatoria.");
            }
            this.udpPort = Integer.parseInt(udpPortStr.trim());

            this.userDbPath = properties.getProperty("persistence.users.path", "data/users.json");
            this.gameDbPath = properties.getProperty("persistence.games.path", "data/games.json");
            this.wordsFilePath = properties.getProperty("game.words.path", "data/words.json");

            String flushIntervalStr = properties.getProperty("persistence.flush.interval.minutes", "5");
            this.flushInterval = Long.parseLong(flushIntervalStr.trim());

            String gameDurationStr = properties.getProperty("game.duration.minutes", "10");
            this.gameDurationMinutes = Long.parseLong(gameDurationStr.trim());

            validate();

        } catch (IllegalArgumentException e) {
            throw new IOException("Configurazione non valida: " + e.getMessage(), e);
        }
    }

    private void validate() {
        if (tcpPort < 1024 || tcpPort > 65535) {
            throw new IllegalArgumentException("La porta TCP deve essere compresa tra 1024 e 65535 (valore: " + tcpPort + ").");
        }
        if (udpPort < 1024 || udpPort > 65535) {
            throw new IllegalArgumentException("La porta UDP deve essere compresa tra 1024 e 65535 (valore: " + udpPort + ").");
        }
        if (tcpPort == udpPort) {
            throw new IllegalArgumentException("La porta TCP e la porta UDP non possono coincidere.");
        }
        if (flushInterval <= 0) {
            throw new IllegalArgumentException("L'intervallo di flush deve essere maggiore di zero.");
        }
        if (gameDurationMinutes <= 0) {
            throw new IllegalArgumentException("La durata della partita deve essere maggiore di zero.");
        }
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public String getUserDbPath() {
        return userDbPath;
    }

    public String getGameDbPath() {
        return gameDbPath;
    }

    public String getWordsFilePath() {
        return wordsFilePath;
    }

    public long getFlushInterval() {
        return flushInterval;
    }

    public long getGameDurationMinutes() {
        return gameDurationMinutes;
    }
}