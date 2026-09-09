package common.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Rappresenta un'istanza di una sessione di gioco.
 * <p>
 * Incapsula il modello di base ({@link GameTemplate}), l'elenco delle 16 parole mescolate
 * da distribuire ai client e la finestra temporale di validità.
 * Classe immutabile e thread-safe.
 */
public class Game {
    private final GameTemplate gameTemplate;
    private final List<String> shuffledWords;
    private final long startTime;
    private final long endTime;

    /**
     * Crea un'istanza di partita specificando esplicitamente parole e limiti temporali.
     *
     * @param gameTemplate template di definizione dei gruppi della partita
     * @param shuffledWords lista delle parole in ordine casuale da inviare ai giocatori
     * @param startTime timestamp di inizio partita in millisecondi
     * @param endTime timestamp di fine partita in millisecondi
     */
    public Game(GameTemplate gameTemplate, List<String> shuffledWords, long startTime, long endTime) {
        this.gameTemplate = gameTemplate;
        this.startTime = startTime;
        this.endTime = endTime;
        
        if (shuffledWords != null) {
            this.shuffledWords = Collections.unmodifiableList(new ArrayList<>(shuffledWords));
        } else {
            this.shuffledWords = Collections.emptyList();
        }
    }

    /**
     * Costruisce una nuova partita estraendo e mescolando le parole dal template
     * e impostando l'intervallo temporale a partire dall'istante corrente.
     *
     * @param gameTemplate template con i 4 gruppi della partita
     * @param durationMillis durata complessiva della sessione in millisecondi
     */
    public Game(GameTemplate gameTemplate, long durationMillis) {
        this.gameTemplate = gameTemplate;
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime + durationMillis;

        List<String> tempWords = new ArrayList<>();
        for (WordGroup group : this.gameTemplate.getGroups()) {
            tempWords.addAll(group.getWords());
        }
        Collections.shuffle(tempWords);
        
        this.shuffledWords = Collections.unmodifiableList(tempWords);
    }

    public GameTemplate getGameTemplate() {
        return gameTemplate;
    }

    /**
     * Restituisce la lista immutabile delle parole mescolate associate a questa partita.
     * <p>
     * Query pura (nessun effetto collaterale).
     *
     * @return vista non modificabile della lista di parole
     */
    public List<String> getShuffledWords() {
        if (this.shuffledWords == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.shuffledWords);
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}