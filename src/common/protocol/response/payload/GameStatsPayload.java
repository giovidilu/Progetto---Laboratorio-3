package common.protocol.response.payload;

import common.protocol.response.GameState;

/**
 * Payload di risposta per l'operazione {@code requestGameStats}, contenente le statistiche
 * aggregate globali di una partita (in corso o archiviata).
 * <p>
 * Espone metriche differenti in base allo stato: tempo rimanente e giocatori attivi/conclusi
 * se la partita è in corso; partecipanti totali, completati, vittorie e media punteggio se terminata.
 * <p>
 * Classe immutabile e thread-safe.
 */
public class GameStatsPayload {

    private final GameState state;

    // Campi specifici per partita in corso
    private final Integer timeRemaining;
    private final Integer playersStillPlaying;
    private final Integer playersFinished;
    private final Integer playersWon;

    // Campi specifici per partita conclusa
    private final Integer totalParticipants;
    private final Integer participantsFinished;
    private final Integer participantsWon;
    private final Double averageScore;

    /**
     * Costruttore privato; l'istanziazione avviene tramite factory method statici dedicati.
     */
    private GameStatsPayload(GameState state, Integer timeRemaining, Integer playersStillPlaying,
                              Integer playersFinished, Integer playersWon, Integer totalParticipants,
                              Integer participantsFinished, Integer participantsWon, Double averageScore) {
        this.state = state;
        this.timeRemaining = timeRemaining;
        this.playersStillPlaying = playersStillPlaying;
        this.playersFinished = playersFinished;
        this.playersWon = playersWon;
        this.totalParticipants = totalParticipants;
        this.participantsFinished = participantsFinished;
        this.participantsWon = participantsWon;
        this.averageScore = averageScore;
    }

    /**
     * Costruisce il payload con le statistiche aggregate di una partita in corso.
     *
     * @param timeRemaining secondi rimanenti prima del termine globale
     * @param playersStillPlaying numero di giocatori che non hanno ancora concluso il puzzle
     * @param playersFinished numero di giocatori che hanno completato la sessione in anticipo
     * @param playersWon numero di giocatori che hanno completato con esito positivo
     * @return istanza di {@link GameStatsPayload} configurata per match attivo
     */
    public static GameStatsPayload ongoingGame(Integer timeRemaining, Integer playersStillPlaying,
                                                Integer playersFinished, Integer playersWon) {
        return new GameStatsPayload(GameState.ONGOING, timeRemaining, playersStillPlaying,
                playersFinished, playersWon, null, null, null, null);
    }

    /**
     * Costruisce il payload con le statistiche consolidate di una partita conclusa.
     *
     * @param totalParticipants totale dei partecipanti che hanno preso parte alla partita
     * @param participantsFinished partecipanti che hanno completato il puzzle entro i limiti
     * @param participantsWon partecipanti che hanno vinto la partita
     * @param averageScore punteggio medio calcolato su tutti i partecipanti registrati alla partita
     * @return istanza di {@link GameStatsPayload} configurata per match archiviato
     */
    public static GameStatsPayload finishedGame(Integer totalParticipants, Integer participantsFinished,
                                                 Integer participantsWon, Double averageScore) {
        return new GameStatsPayload(GameState.FINISHED, null, null, null, null,
                totalParticipants, participantsFinished, participantsWon, averageScore);
    }

    /** Restituisce lo stato temporale della partita. */
    public GameState getState() { return state; }

    /** Restituisce i secondi residui prima della conclusione della partita attiva. */
    public Integer getTimeRemaining() { return timeRemaining; }

    /** Restituisce il numero di partecipanti con la partita ancora in corso. */
    public Integer getPlayersStillPlaying() { return playersStillPlaying; }

    /** Restituisce il numero di partecipanti che hanno concluso il turno in corso. */
    public Integer getPlayersFinished() { return playersFinished; }

    /** Restituisce il numero di partecipanti che hanno concluso con una vittoria il turno in corso. */
    public Integer getPlayersWon() { return playersWon; }

    /** Restituisce il totale complessivo dei partecipanti registrati alla partita terminata. */
    public Integer getTotalParticipants() { return totalParticipants; }

    /** Restituisce il totale dei partecipanti che hanno completato la partita terminata. */
    public Integer getParticipantsFinished() { return participantsFinished; }

    /** Restituisce il totale dei partecipanti che hanno vinto la partita terminata. */
    public Integer getParticipantsWon() { return participantsWon; }

    /** Restituisce la media aritmetica dei punteggi ottenuti da tutti i partecipanti. */
    public Double getAverageScore() { return averageScore; }
}