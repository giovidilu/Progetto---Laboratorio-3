package client.cli;

import java.io.IOException;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import client.network.ServerConnection;
import common.protocol.request.LoginRequest;
import common.protocol.request.LogoutRequest;
import common.protocol.request.RegisterRequest;
import common.protocol.request.RequestGameInfoRequest;
import common.protocol.request.RequestGameStatsRequest;
import common.protocol.request.RequestLeaderboardRequest;
import common.protocol.request.RequestPlayerStatsRequest;
import common.protocol.request.SubmitProposalRequest;
import common.protocol.request.UpdateCredentialsRequest;
import common.protocol.response.GameState;
import common.protocol.response.ResponseCode;
import common.protocol.response.ServerResponse;
import common.protocol.response.payload.GameInfoPayload;
import common.protocol.response.payload.GameStatsPayload;
import common.protocol.response.payload.LeaderboardEntry;
import common.protocol.response.payload.LeaderboardPayload;
import common.protocol.response.payload.LoginPayload;
import common.protocol.response.payload.MistakeHistogram;
import common.protocol.response.payload.PlayerStatsPayload;

public class CommandLineInterface {
    private final ServerConnection serverConnection;
    private final Scanner scanner;

    private boolean loggedIn;
    private String currentUsername;

    public CommandLineInterface(ServerConnection serverConnection, Scanner scanner) {
        this.serverConnection = serverConnection;
        this.scanner = scanner;
        this.loggedIn = false;
        this.currentUsername = null;
    }

    public void run() {
        boolean running = true;
        
        System.out.println("=== Benvenuto in Connections ===");
        
        while (running) {
            printMenu();
            
            try {
                System.out.print("\nSeleziona un'opzione: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consuma il newline rimasto nel buffer
                
                // Deleghiamo la gestione della scelta a un metodo separato
                running = handleChoice(choice);
                
            } catch (InputMismatchException e) {
                System.out.println("Errore: Inserisci un numero intero valido.");
                scanner.nextLine(); // Pulisce il buffer dallo scarto
            }
        }
        
        System.out.println("Chiusura del client in corso...");
    }

    private void printMenu() {
        System.out.println("\n--- MENU ---");
        if (!loggedIn) {
            System.out.println("1. Registrazione");
            System.out.println("2. Login");
            System.out.println("0. Esci");
        } else {
            System.out.println("1. Invia Proposta");
            System.out.println("2. Richiedi Stato Partita");
            System.out.println("3. Logout");
            System.out.println("4. Aggiorna Credenziali");
            System.out.println("5. Statistiche Partita");
            System.out.println("6. Classifica");
            System.out.println("7. Statistiche Personali");
        }
    }

    private boolean handleChoice(int choice) {
        
        if (!loggedIn) {
            switch (choice) {
                case 1: {
                    System.out.print("Username: ");
                    String username = scanner.nextLine();
                    System.out.print("Password: ");
                    String psw = scanner.nextLine();

                    RegisterRequest request = new RegisterRequest(username, psw);
                    
                    try {
                        serverConnection.sendRequest(request);

                        Type responseType = new TypeToken<ServerResponse<Void>>(){}.getType();
                        ServerResponse<Void> response = serverConnection.receiveResponse(responseType);

                        if (response.getStatus() == ResponseCode.SUCCESS) {
                            System.out.println("Registrazione avvenuta con successo!");
                        } else {
                            System.out.println("Registrazione fallita: " + response.getStatus());
                            if (response.getMessage() != null) {
                                System.out.println("Dettaglio: " + response.getMessage());
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Errore di comunicazione con il server: " + e.getMessage());
                    }
                    return true;
                }
                case 2: {
                    System.out.print("Username: ");
                    String username = scanner.nextLine();
                    System.out.print("Password: ");
                    String psw = scanner.nextLine();
                    
                    LoginRequest request = new LoginRequest(username, psw);
                    
                    try {
                        serverConnection.sendRequest(request);
                        
                        Type responseType = new TypeToken<ServerResponse<LoginPayload>>(){}.getType();
                        ServerResponse<LoginPayload> response = serverConnection.receiveResponse(responseType);
                        
                        if (response.getStatus() == ResponseCode.SUCCESS) {
                            this.loggedIn = true;
                            this.currentUsername = username;
                            System.out.println("Login effettuato con successo!");
                            
                            LoginPayload payload = response.getPayload();
                            
                            if (payload != null) {
                                System.out.println("Parole della partita: " + payload.getWords());
                                System.out.println("Errori commessi: " + payload.getErrors());
                                System.out.println("Tempo rimanente: " + payload.getTimeRemaining());
                                System.out.println("Punteggio corrente: " + payload.getScore());
                            }
                        } else {
                            System.out.println("Login fallito: " + response.getStatus());
                            if (response.getMessage() != null) {
                                System.out.println("Dettaglio: " + response.getMessage());
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Errore di comunicazione con il server: " + e.getMessage());
                    }
                    return true;
                }
                
                case 0:
                    return false;
                    
                default:
                    System.out.println("Opzione non valida. Riprova.");
                    return true;
                }
            } else {
                switch (choice) {
                    case 1: {
                        System.out.println("Inserisci le 4 parole della proposta, separate da virgola:");
                        String line = scanner.nextLine();
                        
                        List<String> words = Arrays.asList(line.split("\\s*,\\s*"));
                        
                        SubmitProposalRequest request = new SubmitProposalRequest(words);
                        
                        try {
                            serverConnection.sendRequest(request);
                            
                            Type responseType = new TypeToken<ServerResponse<Void>>(){}.getType();
                            
                            ServerResponse<Void> response = serverConnection.receiveResponse(responseType);
                            
                            if (response.getStatus() == ResponseCode.SUCCESS) {
                                System.out.println("Proposta corretta!");
                            } else {
                                System.out.println("Proposta errata: " + response.getStatus());
                                if (response.getMessage() != null) {
                                    System.out.println("Dettaglio: " + response.getMessage());
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Errore di comunicazione con il server: " + e.getMessage());
                        }
                        
                        return true;
                    }
                    
                    case 2: {
                        RequestGameInfoRequest request = new RequestGameInfoRequest();
                        
                        try {
                            serverConnection.sendRequest(request);
                            
                            Type responseType = new TypeToken<ServerResponse<GameInfoPayload>>(){}.getType();
                            ServerResponse<GameInfoPayload> response = serverConnection.receiveResponse(responseType);
                            
                            if (response.getStatus() == ResponseCode.SUCCESS) {
                                
                                GameInfoPayload payload = response.getPayload();
                                System.out.println("Stato partita: " + payload.getState());
                                System.out.println("Errori: " + payload.getErrors());
                                System.out.println("Punteggio: " + payload.getScore());
                            } else {
                                System.out.println("Richiesta fallita: " + response.getStatus());
                            }
                        } catch (IOException e) {
                            System.out.println("Errore di comunicazione con il server: " + e.getMessage());
                        }
                        
                        return true;
                    }
                    
                    case 3: {
                        LogoutRequest request = new LogoutRequest();
                        
                        try {
                            serverConnection.sendRequest(request);
                            
                            Type responseType = new TypeToken<ServerResponse<Void>>(){}.getType();
                            ServerResponse<Void> response = serverConnection.receiveResponse(responseType);
                            
                            if (response.getStatus() == ResponseCode.SUCCESS) {
                                this.loggedIn = false;
                                this.currentUsername = null;
                                
                                System.out.println("Logout effettuato.");
                            } else {
                                System.out.println("Logout fallito: " + response.getStatus());
                            }
                        } catch (IOException e) {
                            System.out.println("Errore di comunicazione con il server: " + e.getMessage());
                        }
                        return true;
                    }

                    case 4: {
                        System.out.println("\n--- Aggiornamento Credenziali ---");
                        System.out.println("Cosa desideri aggiornare?");
                        System.out.println("1. Solo Username");
                        System.out.println("2. Solo Password");
                        System.out.println("3. Entrambi");
                        System.out.print("Scelta: ");
                        
                        int updateChoice;
                        try {
                            updateChoice = scanner.nextInt();
                            scanner.nextLine();
                        } catch (InputMismatchException e) {
                            System.out.println("Errore: Inserisci un numero intero valido.");
                            scanner.nextLine();
                            return true;
                        }
                        
                        if (updateChoice < 1 || updateChoice > 3) {
                            System.out.println("Scelta non valida.");
                            return true;
                        }
                        System.out.print("Inserisci l'attuale username: ");
                        String oldUsername = scanner.nextLine();
                        System.out.print("Inserisci l'attuale password: ");
                        String oldPsw = scanner.nextLine();
                        
                        UpdateCredentialsRequest request = null;
                        String newUsername = null;
                        
                        if (updateChoice == 1) {
                            System.out.print("Inserisci il nuovo username: ");
                            newUsername = scanner.nextLine();
                            request = UpdateCredentialsRequest.forUsernameUpdate(oldUsername, oldPsw, newUsername);
                        } else if (updateChoice == 2) {
                            System.out.print("Inserisci la nuova password: ");
                            String newPsw = scanner.nextLine();
                            request = UpdateCredentialsRequest.forPasswordUpdate(oldUsername, oldPsw, newPsw);
                        } else if (updateChoice == 3) {
                            System.out.print("Inserisci il nuovo username: ");
                            newUsername = scanner.nextLine();
                            System.out.print("Inserisci la nuova password: ");
                            String newPsw = scanner.nextLine();
                            request = UpdateCredentialsRequest.forBothUpdate(oldUsername, oldPsw, newUsername, newPsw);
                        }
                        
                        try {
                            serverConnection.sendRequest(request);
                            Type responseType = new TypeToken<ServerResponse<Void>>(){}.getType();
                            ServerResponse<Void> response = serverConnection.receiveResponse(responseType);
                            
                            if (response.getStatus() == ResponseCode.SUCCESS) {
                                // Lo stato di login non cambia: l'utente resta autenticato
                                if (newUsername != null) {
                                    this.currentUsername = newUsername;
                                }
                                
                                System.out.println("Credenziali aggiornate con successo!");
                            } else {
                                System.out.println("Aggiornamento fallito: " + response.getStatus());
                                if (response.getMessage() != null) {
                                    System.out.println("Dettaglio: " + response.getMessage());
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Errore di comunicazione con il server: " + e.getMessage());
                            
                        }
                        
                        return true;
                    }

                    case 5: {
                        System.out.println("\n--- Statistiche Partita ---");
                        System.out.print("Vuoi le statistiche della partita corrente (C) o di una passata (P)? ");
                        String choiceGame = scanner.nextLine().trim().toUpperCase();
                                        
                        Integer gameId = null;
                                        
                        if (choiceGame.equals("P")) {
                            System.out.print("Inserisci l'ID della partita: ");
                            try {
                                gameId = scanner.nextInt();
                                scanner.nextLine();
                            } catch (InputMismatchException e) {
                                System.out.println("Errore: ID partita non valido. Devi inserire un numero intero.");
                                scanner.nextLine();
                                return true;
                            }
                        } else if (!choiceGame.equals("C")) {
                            System.out.println("Scelta non valida. Operazione annullata");
                            return true;
                        }
                    
                        // Scelta del costruttore corretto in base alla presenza o meno dell'id
                        RequestGameStatsRequest request;
                        if (gameId != null) {
                            request = new RequestGameStatsRequest(gameId);
                        } else {
                            request = new RequestGameStatsRequest();
                        }
                    
                        try {
                            serverConnection.sendRequest(request);
                            Type responseType = new TypeToken<ServerResponse<GameStatsPayload>>(){}.getType();
                            ServerResponse<GameStatsPayload> response = serverConnection.receiveResponse(responseType);
                        
                            if (response.getStatus() == ResponseCode.SUCCESS) {
                                GameStatsPayload payload = response.getPayload();
                            
                                if (payload.getState() == GameState.ONGOING) {
                                    System.out.println("--- Statistiche Partita in Corso ---");
                                    System.out.println("Tempo rimanente: " + payload.getTimeRemaining());
                                    System.out.println("Giocatori ancora in gioco: " + payload.getPlayersStillPlaying());
                                    System.out.println("Giocatori che hanno concluso: " + payload.getPlayersFinished());
                                    System.out.println("Vittorie: " + payload.getPlayersWon());
                                } else {
                                    System.out.println("--- Statistiche Partita Conclusa ---");
                                    System.out.println("Giocatori partecipanti totali: " + payload.getTotalParticipants());
                                    System.out.println("Giocatori che hanno concluso: " + payload.getParticipantsFinished());
                                    System.out.println("Vittorie: " + payload.getParticipantsWon());
                                    System.out.println("Punteggio medio: " + payload.getAverageScore());
                                }
                            } else {
                                System.out.println("Richiesta statistiche fallita: " + response.getStatus());
                                if (response.getMessage() != null) {
                                    System.out.println("Dettaglio: " + response.getMessage());
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Errore di comunicazione con il server: " + e.getMessage());
                        }
                    
                        return true;
                    }

                    case 6: {
                        System.out.println("\n--- Classifica ---");
                        System.out.println("Quale classifica desideri visualizzare?");
                        System.out.println("1. Tutti i giocatori");
                        System.out.println("2. Top k giocatori");
                        System.out.println("3. Posizione di uno specifico giocatore");
                        System.out.print("Scelta: ");
                        int leadChoice;
                        
                        try{
                            leadChoice = scanner.nextInt();
                            scanner.nextLine();
                        } catch (InputMismatchException e){
                            System.out.println("Errore: Inserisci un numero intero valido.");
                            scanner.nextLine();
                            return true;
                        }
                        
                        RequestLeaderboardRequest request = null;
                        
                        if(leadChoice == 1){
                            request = RequestLeaderboardRequest.forAllPlayers();
                        } else if(leadChoice == 2){
                            System.out.print("Inserisci il numero di giocatori (K) da visualizzare: ");
                            int k;
                            try {
                                k = scanner.nextInt();
                                scanner.nextLine();
                            } catch (InputMismatchException e){
                                System.out.println("Errore: Inserisci un numero intero valido.");
                                scanner.nextLine();
                                return true;
                            }
                            request = RequestLeaderboardRequest.forTopPlayers(k);
                        } else if(leadChoice == 3){
                            System.out.print("Inserisci il nome del giocatore: ");
                            String player = scanner.nextLine();
                            request = RequestLeaderboardRequest.forPlayer(player);
                        } else {
                            System.out.println("Scelta non valida. Operazione annullata");
                            return true;
                        }

                        try {
                            serverConnection.sendRequest(request);
                            Type responseType = new TypeToken<ServerResponse<LeaderboardPayload>>(){}.getType();
                            ServerResponse<LeaderboardPayload> response = serverConnection.receiveResponse(responseType);
                    
                            if (response.getStatus() == ResponseCode.SUCCESS) {
                                LeaderboardPayload payload = response.getPayload();
                                System.out.println("--- Risultati Classifica ---");
                                
                                if(payload.getEntries() != null && !payload.getEntries().isEmpty()){
                                    for(LeaderboardEntry entry: payload.getEntries()){
                                        System.out.println(entry.getRank() + ". " +  entry.getUsername() + " - " + entry.getScore() + " punti");
                                    }
                                } else {
                                    System.out.println("La classifica è attualmente vuota.");
                                }
                            } else {
                                System.out.println("Richiesta classifica fallita: " + response.getStatus());
                                if (response.getMessage() != null) {
                                    System.out.println("Dettaglio: " + response.getMessage());
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Errore di comunicazione con il server: " + e.getMessage());
                        }
                        
                        return true;
                    }

                    case 7: {
                        System.out.println("\n--- Statistiche Personali ---");

                        RequestPlayerStatsRequest request = new RequestPlayerStatsRequest();

                        try{
                            serverConnection.sendRequest(request);
                            Type responseType = new TypeToken<ServerResponse<PlayerStatsPayload>>(){}.getType();
                            ServerResponse<PlayerStatsPayload> response = serverConnection.receiveResponse(responseType);
                    
                            if (response.getStatus() == ResponseCode.SUCCESS) {
                                PlayerStatsPayload payload = response.getPayload();
                                
                                System.out.println("Partite completate: " + payload.getPuzzlesCompleted());
                                System.out.println("Percentuale vittorie: " + payload.getWinRate() + "%");
                                System.out.println("Percentuale sconfitte: " + payload.getLossRate() + "%");
                                System.out.println("Serie di vittorie attuale: " + payload.getCurrentStreak());
                                System.out.println("Serie massima: " + payload.getMaxStreak());
                                System.out.println("Partite perfette (0 errori): " + payload.getPerfectPuzzles());

                                System.out.println("\n--- Istogramma Errori ---");
                                MistakeHistogram histogram = payload.getMistakeHistogram();
                                
                                if (histogram != null) {
                                    System.out.println("Risolte con 0 errori: " + histogram.getSolvedWith0Mistakes());
                                    System.out.println("Risolte con 1 errore: " + histogram.getSolvedWith1Mistake());
                                    System.out.println("Risolte con 2 errori: " + histogram.getSolvedWith2Mistakes());
                                    System.out.println("Risolte con 3 errori: " + histogram.getSolvedWith3Mistakes());
                                    System.out.println("Risolte con 4 errori: " + histogram.getSolvedWith4Mistakes());
                                    System.out.println("Fallite (4 errori raggiunti): " + histogram.getFailed());
                                    System.out.println("Non concluse in tempo: " + histogram.getNotFinished());
                                } else {
                                    System.out.println("Dati dell'istogramma non disponibili.");
                                }
                            } else {
                                System.out.println("Richiesta statistiche personali fallita: " + response.getStatus());
                                if(response.getMessage() != null){
                                    System.out.println("Dettaglio: " + response.getMessage());
                                }
                            }
                        } catch(IOException e){
                            System.out.println("Errore di comunicazione con il server: " + e.getMessage());
                        }

                        return true;
                    }

                    default:
                        System.out.println("Opzione non valida. Riprova.");
                        return true;
                }
            }
        }
    }
