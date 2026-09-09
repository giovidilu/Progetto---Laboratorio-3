package common.dto;

import java.util.HashSet;
import java.util.List;

/**
 * Oggetto di trasferimento dati (DTO): rappresenta una proposta di quadrupla di parole inviata da un giocatore.
 */
public class SubmitProposalRequest {
    private List<String> words;

    /** Costruttore vuoto per deserializzazione JSON. */
    public SubmitProposalRequest() {
    }

    /** Costruisce la richiesta a partire da una lista di vocaboli. */
    public SubmitProposalRequest(List<String> words) {
        this.words = words;
    }

    /** Restituisce l'elenco dei termini della proposta. */
    public List<String> getWords() {
        return words;
    }

    /**
     * Verifica la conformità sintattica della proposta secondo le regole di gioco.
     * <p>
     * Controlla che la lista sia composta da esattamente 4 termini distinti e non vuoti.
     * Query pura (nessun effetto collaterale).
     *
     * @return {@code true} se la proposta contiene esattamente 4 parole uniche non vuote, {@code false} altrimenti
     */
    public boolean isValid(){
        if(words == null || words.size() != 4){
            return false;
        }

        for(String word: words){
            if(word == null || word.isBlank()){
                return false;
            }
        }

        return new HashSet<>(words).size() == 4;
    }
}