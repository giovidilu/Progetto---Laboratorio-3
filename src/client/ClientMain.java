package client;

import java.util.Scanner;

import client.cli.CommandLineInterface;
import client.config.ClientConfig;
import client.network.ServerConnection;

/**
 * Entry point dell'applicazione client per il gioco Connections.
 * <p>
 * Inizializza la configurazione di rete leggendo il file di configurazione statico,
 * stabilisce la connessione con il server e avvia l'interfaccia testuale interattiva
 */
public class ClientMain {
    private static final String CONFIG_PATH = "config/client.properties";
    
    /**
     * Punto di ingresso principale del processo client.
     *
     * @param args argomenti passati da riga di comando (non utilizzati)
     */
    public static void main(String[] args){
        System.out.println("Avvio del client in corso...");

        try{
            ClientConfig config = new ClientConfig(CONFIG_PATH);

            try(ServerConnection serverConnection = new ServerConnection(config.getServerHost(),config.getServerPort())){
                Scanner scanner = new Scanner(System.in);

                CommandLineInterface cli = new CommandLineInterface(serverConnection, scanner);
                cli.run();
            }
        } catch (Exception e){
            System.out.println("\nErrore fatale: impossibile avviare o mantenere in esecuzione il client");
            System.out.println("Dettaglio tecnico: " + e.getMessage());
            System.out.println("Verifica che il file di configurazione sia presente e che il server sia attivo");

            System.exit(1);
        }
    }
}