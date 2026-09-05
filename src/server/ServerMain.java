package server;

import server.config.ServerConfig;
import server.handler.ClientHandler;
import server.repository.GameRepository;
import server.repository.GameTemplateLoader;
import server.repository.UserRepository;
import server.service.GameManager;
import server.service.PersistenceManager;
import server.service.SessionManager;

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
 * Classe principale del Server di gioco "Connections".
 * Si occupa dell'inizializzazione dei repository, dell'avvio dei servizi
 * di persistenza, della gestione del ciclo di vita del ServerSocket TCP
 * e del dispatching concorrente delle connessioni client verso un thread pool.
 */
public class ServerMain {
    private final ServerConfig config;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final SessionManager sessionManager;
    private final PersistenceManager persistenceManager;
    private final GameManager gameManager;

    private final ExecutorService clientThreadPool;
    private ServerSocket serverSocket;
    private volatile boolean running;

    /**
     * Costruttore: carica i dati persistenti da disco (fail-fast) e
     * inizializza le strutture dati condivise e il thread pool.
     *
     * @param config Configurazione caricata da file properties.
     * @throws IOException Se il caricamento dei dati da disco fallisce.
     */
    public ServerMain(ServerConfig config) throws IOException {
        this.config = config;

        // 1. Inizializzazione e caricamento dati su memoria centrale
        this.userRepository = new UserRepository(config.getUserDbPath());
        this.gameRepository = new GameRepository(config.getGameDbPath());

        this.userRepository.loadFromDisk();
        this.gameRepository.loadFromDisk();

        GameTemplateLoader templateLoader = new GameTemplateLoader();
        Map<Integer,GameTemplate> templates = templateLoader.loadTemplates(config.getWordsFilePath());

        long gameDurationMillis = TimeUnit.MINUTES.toMillis(config.getGameDurationMinutes());
        this.gameManager = new GameManager(
            templates, 
            this.gameRepository, 
            this.userRepository,
            gameDurationMillis
        );

        // 2. Inizializzazione del gestore delle sessioni attive
        this.sessionManager = new SessionManager();

        // 3. Inizializzazione del gestore del salvataggio periodico su disco
        this.persistenceManager = new PersistenceManager(
            this.userRepository,
            this.gameRepository,
            config.getFlushInterval(),
            TimeUnit.MINUTES
        );

        // 4. Thread pool dinamico per gestire i thread worker dei client
        this.clientThreadPool = Executors.newCachedThreadPool();
        this.running = true;
    }

    /**
     * Avvia il server TCP e i relativi servizi di background.
     *
     * @throws IOException Se l'acquisizione della porta TCP fallisce (es. BindException).
     */
    public void start() throws IOException {
        // Registrazione dello Shutdown Hook per la terminazione pulita
        registerShutdownHook();

        // Apertura del socket TCP sulla porta configurata
        this.serverSocket = new ServerSocket(config.getTcpPort());
        System.out.println("[SERVER] In ascolto sulla porta TCP: " + config.getTcpPort());

        // Avvio del timer periodico per la persistenza
        this.persistenceManager.start();
        this.gameManager.start();

        // Ciclo bloccante di ascolto e accettazione connessioni
        runServerLoop();
    }

    
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
                // Quando serverSocket viene chiuso durante lo shutdown, viene sollevata una SocketException
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

            gameManager.stop();
            persistenceManager.stop();

            System.out.println("[SHUTDOWN] Server terminato correttamente.");
        }));
    }

    public static void main(String[] args) {
        String configPath = "config/server.properties";
        if (args.length > 0) {
            configPath = args[0];
        }

        try {
            ServerConfig config = new ServerConfig(configPath);
            ServerMain server = new ServerMain(config);
            server.start();
        } catch (IOException e) {
            System.err.println("[FATAL] Impossibile avviare il server: " + e.getMessage());
            System.exit(1);
        }
    }
}