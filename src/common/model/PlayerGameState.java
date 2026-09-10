package common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rappresenta lo stato e l'avanzamento individuale di un giocatore nella partita.
 * <p>
 * Traccia i gruppi individuati, gli errori commessi e determina dinamicamente punteggio ed esito.
 * Non è thread-safe: l'accesso e la mutazione devono essere coordinati tramite sincronizzazione esterna.
 */
public class PlayerGameState {
    private final String username;
    private final int gameId;
    private final List<WordGroup> correctGroups;
    private int mistakes;

    /**
     * Inizializza lo stato di gioco per l'utente azzerando errori e gruppi risolti.
     *
     * @param username identificativo del giocatore
     * @param gameId identificativo della sessione di gioco
     */
    public PlayerGameState(String username, int gameId){
        this.username = username;
        this.gameId = gameId;
        this.correctGroups = new ArrayList<>();
        this.mistakes = 0;
    }

    /** Restituisce il nome utente del giocatore. */
    public String getUsername(){
        return username;
    }

    /** Restituisce l'identificativo del gioco cui fa riferimento lo stato. */
    public int getGameId(){
        return gameId;
    }

    /** Restituisce la vista immutabile dell'elenco di gruppi corretti trovati. */
    public List<WordGroup> getCorrectGroups(){
        return Collections.unmodifiableList(correctGroups);
    }

    /** Restituisce il numero di errori commessi finora. */
    public int getMistakes(){
        return mistakes;
    }

    /**
     * Aggiunge un gruppo indovinato all'elenco di quelli risolti se non già presente.
     * <p>
     * Metodo con effetto collaterale (modifica lo stato interno).
     *
     * @param group gruppo tematico corretto da registrare
     */
    public void addCorrectGroup(WordGroup group){
        if(group != null && !this.correctGroups.contains(group)){
            this.correctGroups.add(group);
        }
    }

    /**
     * Incrementa di un'unità gli errori commessi dall'utente nella partita.
     * <p>
     * Metodo con effetto collaterale.
     *
     * @throws IllegalStateException se l'utente ha già raggiunto il limite di 4 errori consentiti
     */
    public void incrementMistakes() {
        if (this.mistakes >= 4) {
            throw new IllegalStateException("Raggiunto il limite massimo di 4 errori consentiti.");
        }
        this.mistakes++;
    }

    /**
     * Calcola il punteggio attuale in base ai gruppi indovinati e agli errori commessi.
     * <p>
     * Formula applicata da specifica: +6 per ogni gruppo corretto (fino a 3) e -4 per ogni errore.
     * Il conteggio dei gruppi validi ai fini del punteggio è limitato a 3 ({@code Math.min(size, 3)})
     * perché, alla vittoria con 3 gruppi risolti esplicitamente, il 4° gruppo rimanente viene
     * aggiunto automaticamente a {@code correctGroups} (vedi {@link #addCorrectGroup}) per motivi di
     * coerenza dello stato archiviato: senza questo limite risulterebbero erroneamente assegnati
     * punti anche per un gruppo che il giocatore non ha sottomesso.
     * Query pura (nessun effetto collaterale).
     *
     * @return punteggio corrente del giocatore
     */
    public int getScore(){
        int scoredGroups = Math.min(this.correctGroups.size(), 3);
        int pointsFromCorrect = scoredGroups * 6;
        int pointsFromMistakes = this.mistakes * -4;
        return pointsFromCorrect + pointsFromMistakes;
    }

    /**
     * Valuta l'esito della partita per il giocatore in base allo stato raggiunto.
     * <p>
     * Query pura (nessun effetto collaterale).
     *
     * @return {@link GameOutcome#WON} con almeno 3 gruppi indovinati, {@link GameOutcome#LOST_BY_MISTAKES}
     *         con 4 errori commessi, oppure {@code null} se la partita è ancora aperta
     */
    public GameOutcome getOutcome(){
        if (this.correctGroups != null && this.correctGroups.size() >= 3) {
            return GameOutcome.WON;
        }
        if(this.mistakes >= 4){
            return GameOutcome.LOST_BY_MISTAKES;
        }

        return null;
    }
}