package server.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Gestore immutabile della configurazione del server.
 * <p>
 * Carica e valida all'avvio i parametri di rete (porte TCP e UDP), i percorsi
 * di persistenza JSON e i vincoli temporali del gioco (durata turni, intervallo di flush).
 * Classe thread-safe: dopo la costruzione lo stato interno è interamente in sola lettura.
 */
public class ServerConfig {
    private final String serverHost;
    private final int tcpPort;
    private final int udpPort;

    private final String userDbPath;
    private final String gameDbPath;
    private final String wordsFilePath;

    private final long flushInterval;
    private final long gameDurationMinutes;

    /**
     * Inizializza la configurazione caricando i valori dal file .properties specificato.
     *
     * @param configFilePath percorso del file di configurazione su disco
     * @throws IOException se il file non esiste, si verificano errori di lettura o la configurazione non è valida
     */
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

    /**
     * Valida i vincoli di consistenza dei parametri di configurazione.
     */
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

    /** Restituisce l'hostname o indirizzo di ascolto configurato per il server. */
    public String getServerHost() {
        return serverHost;
    }

    /** Restituisce la porta TCP principale per le connessioni client. */
    public int getTcpPort() {
        return tcpPort;
    }

    /** Restituisce la porta UDP configurata per le notifiche asincrone. */
    public int getUdpPort() {
        return udpPort;
    }

    /** Restituisce il percorso del file JSON per la persistenza degli utenti. */
    public String getUserDbPath() {
        return userDbPath;
    }

    /** Restituisce il percorso del file JSON per la persistenza dello storico partite. */
    public String getGameDbPath() {
        return gameDbPath;
    }

    /** Restituisce il percorso del file contenente il dizionario delle parole e categorie. */
    public String getWordsFilePath() {
        return wordsFilePath;
    }

    /** Restituisce l'intervallo periodico (in minuti) per il salvataggio su disco. */
    public long getFlushInterval() {
        return flushInterval;
    }

    /** Restituisce la durata prefissata di ogni singola partita in minuti. */
    public long getGameDurationMinutes() {
        return gameDurationMinutes;
    }
}