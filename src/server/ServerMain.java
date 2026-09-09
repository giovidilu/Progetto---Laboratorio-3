package server;

import server.config.ServerConfig;
import server.handler.ClientHandler;
import server.repository.GameRepository;
import server.repository.GameTemplateLoader;
import server.repository.UserRepository;
import server.service.GameManager;
import server.service.PersistenceManager;
import server.service.SessionManager;
import server.service.UdpNotifier;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import common.model.GameTemplate;

/**
 * Entry point e coordinatore principale del server multithreaded del gioco Connections[cite: 50, 58].
 * <p>
 * Inizializza i repository in memoria dai file JSON, carica i template di gioco, avvia i servizi
 * periodici in background (gestione partite e persistenza) e accetta le connessioni TCP in ingresso
 * inoltrandone l'esecuzione al thread pool dinamico dei worker[cite: 50, 58]. Registra uno shutdown hook
 * per rilasciare ordinatamente le risorse di rete ed eseguire il flush finale su disco[cite: 50].
 */
public class ServerMain {
    private final ServerConfig config;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final SessionManager sessionManager;
    private final UdpNotifier udpNotifier;
    private final PersistenceManager persistenceManager;
    private final GameManager gameManager;
    private static final String CONFIG_PATH = "config/server.properties";

    private final ExecutorService clientThreadPool;
    private ServerSocket serverSocket;
    private volatile boolean running;

    /**
     * Inizializza tutti i componenti del server, carica i dati persistenti da disco e predispone il thread pool.
     *
     * @param config configurazione caricata da file properties
     * @throws IOException se si verificano errori nel caricamento da disco o nell'apertura del canale UDP
     */
    public ServerMain(ServerConfig config) throws IOException {
        this.config = config;

        this.userRepository = new UserRepository(config.getUserDbPath());
        this.gameRepository = new GameRepository(config.getGameDbPath());

        this.userRepository.loadFromDisk();
        this.gameRepository.loadFromDisk();

        GameTemplateLoader templateLoader = new GameTemplateLoader();
        Map<Integer, GameTemplate> templates = templateLoader.loadTemplates(config.getWordsFilePath());

        this.sessionManager = new SessionManager();
        this.udpNotifier = new UdpNotifier(config.getUdpPort());

        long gameDurationMillis = TimeUnit.MINUTES.toMillis(config.getGameDurationMinutes());
        this.gameManager = new GameManager(
            templates, 
            this.gameRepository, 
            this.userRepository,
            this.sessionManager,
            this.udpNotifier,
            gameDurationMillis
        );

        this.persistenceManager = new PersistenceManager(
            this.userRepository,
            this.gameRepository,
            config.getFlushInterval(),
            TimeUnit.MINUTES
        );

        this.clientThreadPool = Executors.newCachedThreadPool();
        this.running = true;
    }

    /**
     * Avvia i servizi in background, registra lo shutdown hook e si pone in ascolto bloccante sul socket TCP.
     *
     * @throws IOException se fallisce il bind della porta TCP
     */
    public void start() throws IOException {
        registerShutdownHook();

        this.serverSocket = new ServerSocket(config.getTcpPort());
        System.out.println("[SERVER] In ascolto sulla porta TCP: " + config.getTcpPort() + " e porta UDP: " + config.getUdpPort());

        this.persistenceManager.start();
        this.gameManager.start();

        runServerLoop();
    }

    /**
     * Loop principale bloccante che accetta le connessioni TCP client e le delega al thread pool.
     */
    private void runServerLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();

                clientThreadPool.execute(new ClientHandler(
                    clientSocket,
                    this.userRepository,
                    this.gameRepository,
                    this.sessionManager,
                    this.gameManager
                ));

            } catch (SocketException e) {
                if (!running) {
                    break;
                }
                System.err.println("[SERVER] Errore sul ServerSocket: " + e.getMessage());
            } catch (IOException e) {
                if (!running) {
                    break;
                }
                System.err.println("[SERVER] Errore nell'accettazione della connessione: " + e.getMessage());
            }
        }
    }

    /**
     * Configura il thread di arresto ordinato (shutdown hook) della JVM per la chiusura a cascata delle risorse.
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SHUTDOWN] Arresto del server avviato...");
            this.running = false;

            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    System.err.println("[SHUTDOWN] Errore durante la chiusura del ServerSocket: " + e.getMessage());
                }
            }

            clientThreadPool.shutdown();
            try {
                if (!clientThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    clientThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                clientThreadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }

            if (gameManager != null) {
                gameManager.stop();
            }

            if (udpNotifier != null) {
                udpNotifier.close();
            }

            if (persistenceManager != null) {
                persistenceManager.stop();
            }

            System.out.println("[SHUTDOWN] Server terminato correttamente.");
        }));
    }

    /**
     * Avvia l'applicazione server caricando la configurazione iniziale dal percorso predefinito.
     *
     * @param args argomenti passati da riga di comando (non utilizzati)
     */
    public static void main(String[] args) {
        try {
            ServerConfig config = new ServerConfig(CONFIG_PATH);
            ServerMain server = new ServerMain(config);
            server.start();
        } catch (IOException e) {
            System.err.println("[FATAL] Impossibile avviare il server: " + e.getMessage());
            System.exit(1);
        }
    }
}