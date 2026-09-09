package common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Record storico e statistico di una partita conclusa.
 * <p>
 * Mantiene la soluzione completa dei gruppi, le metriche aggregate di partecipazione
 * e la mappa immutabile degli stati individuali di ciascun partecipante.
 * Classe thread-safe per sola lettura.
 */
public class GameRecord {
    private final int gameId;
    private int totalParticipants;
    private int participantsFinished;
    private int participantsWon;
    private double averageScore;

    private final List<WordGroup> allGroups;
    private final Map<String, PlayerGameState> playerStates;

    /**
     * Inizializza il record storico consolidando le strutture dati interne in collezioni non modificabili.
     *
     * @param gameId identificativo univoco della partita
     * @param totalParticipants numero totale di giocatori che hanno partecipato alla sessione
     * @param participantsFinished numero di giocatori che hanno completato la partita
     * @param participantsWon numero di giocatori che hanno vinto
     * @param averageScore punteggio medio conseguito dai partecipanti
     * @param allGroups elenco dei 4 gruppi costituenti la soluzione
     * @param playerStates mappa degli stati individuali indicizzata per username
     */
    public GameRecord(int gameId, int totalParticipants, int participantsFinished, 
                      int participantsWon, double averageScore, 
                      List<WordGroup> allGroups, Map<String, PlayerGameState> playerStates) {
        this.gameId = gameId;
        this.totalParticipants = totalParticipants;
        this.participantsFinished = participantsFinished;
        this.participantsWon = participantsWon;
        this.averageScore = averageScore;

        if (allGroups != null) {
            this.allGroups = Collections.unmodifiableList(new ArrayList<>(allGroups));
        } else {
            this.allGroups = Collections.emptyList();
        }

        if (playerStates != null) {
            this.playerStates = Collections.unmodifiableMap(new HashMap<>(playerStates));
        } else {
            this.playerStates = Collections.emptyMap();
        }
    }

    public int getGameId() {
        return gameId;
    }

    public int getTotalParticipants() {
        return totalParticipants;
    }

    public int getParticipantsFinished() {
        return participantsFinished;
    }

    public int getParticipantsWon() {
        return participantsWon;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public List<WordGroup> getAllGroups() {
        if (this.allGroups == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.allGroups);
    }

    public Map<String, PlayerGameState> getPlayerStates() {
        if (this.playerStates == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(this.playerStates);
    }
}