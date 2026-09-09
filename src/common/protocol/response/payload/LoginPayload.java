package common.protocol.response.payload;

import java.util.List;

/**
 * Payload di risposta restituito a fronte di un'operazione di {@code login} andata a buon fine.
 * <p>
 * Fornisce al client le informazioni indispensabili per partecipare alla partita globale attiva:
 * insieme delle 16 parole mescolate, eventuali gruppi già indovinati da quell'utente, errori correnti,
 * tempo residuo e punteggio parziale.
 * <p>
 * Classe immutabile e thread-safe.
 */
public class LoginPayload {
    private final List<String> words;
    private final List<List<String>> correctGroups;
    private final int errors;
    private final int timeRemaining;
    private final int score;

    /**
     * Costruisce il payload di benvenuto/ripristino sessione per il giocatore autenticato.
     *
     * @param words insieme completo delle 16 parole della partita corrente
     * @param correctGroups quadruple già risolte dall'utente (se in sessione ripristinata)
     * @param errors numero di proposte errate già effettuate nella partita corrente
     * @param timeRemaining secondi residui prima della conclusione della partita globale
     * @param score punteggio corrente accumulato nella partita
     */
    public LoginPayload(List<String> words, List<List<String>> correctGroups, int errors, int timeRemaining, int score){
        this.words = words;
        this.correctGroups = correctGroups;
        this.errors = errors;
        this.timeRemaining = timeRemaining;
        this.score = score;
    }

    public List<String> getWords(){
        return words;
    }

    public List<List<String>> getCorrectGroups(){
        return correctGroups;
    }
    public int getErrors(){
        return errors;
    }
    public int getTimeRemaining(){
        return timeRemaining;
    }
    public int getScore(){
        return score;
    }
}