package common.protocol.response.payload;

import common.protocol.response.GameState;
import java.util.List;

/**
 * Payload di risposta per l'operazione {@code requestGameInfo}, che descrive lo stato
 * o l'esito di una partita (attiva o storica) per un determinato giocatore.
 * <p>
 * Contiene sia campi comuni (stato, errori commessi, punteggio) sia campi alternativi
 * a seconda che la partita sia in corso (tempo residuo, parole rimanenti, gruppi indovinati)
 * o conclusa (soluzione integrale con ripartizione finale e gruppi totalizzati).
 * <p>
 * Classe immutabile e thread-safe. L'istanziazione è vincolata ai factory method statici.
 */
public class GameInfoPayload {

    private final GameState state;

    // Campi condivisi tra partita in corso e conclusa
    private final Integer errors;
    private final Integer score;

    // Campi specifici per partita in corso
    private final Integer timeRemaining;
    private final List<List<String>> correctGroups;
    private final List<String> words;

    // Campi specifici per partita conclusa
    private final List<List<String>> finalAllocations;
    private final Integer numberCorrectGroups;

    /**
     * Costruttore privato; l'istanziazione avviene tramite factory method statici dedicati.
     */
    private GameInfoPayload(GameState state, Integer errors, Integer  score,
                            Integer  timeRemaining, List<String> words,
                            List<List<String>> correctGroups, List<List<String>> finalAllocations,
                            Integer  numberCorrectGroups) {
        this.state = state;
        this.errors = errors;
        this.score = score;
        this.timeRemaining = timeRemaining;
        this.words = words;
        this.correctGroups = correctGroups;
        this.finalAllocations = finalAllocations;
        this.numberCorrectGroups = numberCorrectGroups;
    }

    /**
     * Costruisce il payload per una partita attualmente in corso.
     *
     * @param timeRemaining secondi residui prima della scadenza della partita
     * @param correctGroups lista dei gruppi di parole già indovinati dal giocatore
     * @param words elenco delle parole residue ancora da raggruppare
     * @param errors numero di proposte errate commesse nella sessione corrente
     * @param score punteggio provvisorio accumulato
     * @return istanza di {@link GameInfoPayload} per partita attiva
     */
    public static GameInfoPayload OngoingGame(Integer timeRemaining, 
                                              List<List<String>> correctGroups, 
                                              List<String> words, 
                                              Integer errors, 
                                              Integer score){
        return  new GameInfoPayload(GameState.ONGOING, errors, score, timeRemaining, words, correctGroups, null, null); 
    }

    /**
     * Costruisce il payload per una partita conclusa (per vittoria, sconfitta o tempo scaduto).
     *
     * @param finalAllocations assegnazione corretta e completa delle 16 parole ai 4 gruppi tematici
     * @param numberCorrectGroups totale dei gruppi corretti individuati dal giocatore
     * @param errors errori totali commessi nel round
     * @param score punteggio finale ottenuto
     * @return istanza di {@link GameInfoPayload} per partita conclusa
     */
    public static GameInfoPayload FinishedGame(List<List<String>> finalAllocations, 
                                               Integer numberCorrectGroups, 
                                               Integer errors, 
                                               Integer score){
        return  new GameInfoPayload(GameState.FINISHED, errors, score, null, null, null, finalAllocations, numberCorrectGroups); 
    }

    /** Restituisce lo stato temporale della partita (ONGOING o FINISHED). */
    public GameState getState(){
        return state;
    }

    /** Restituisce il numero di errori commessi dall'utente nella partita. */
    public Integer getErrors(){
        return errors;
    }

    /** Restituisce il punteggio maturato dall'utente nella partita. */
    public Integer getScore(){
        return score;
    }

    /** Restituisce i secondi rimanenti prima del termine del match (null se concluso). */
    public Integer getTimeRemaining(){
        return timeRemaining;
    }

    /** Restituisce le liste di parole dei gruppi già indovinati dal giocatore. */
    public List<List<String>> getCorrectGroups() {
        return correctGroups;
    }

    /** Restituisce le parole non ancora raggruppate della partita corrente. */
    public List<String> getWords() {
        return words;
    }

    /** Restituisce la composizione integrale corretta dei 4 gruppi di parole. */
    public List<List<String>> getFinalAllocation() {
        return finalAllocations;
    }

    /** Restituisce il conteggio totale di gruppi individuati con successo. */
    public Integer getNumberCorrectGroups() {
        return numberCorrectGroups;
    }

}