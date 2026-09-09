package common.protocol.request;

import java.util.List;

/**
 * Richiesta di protocollo per sottoporre al server un tentativo di 4 parole per la partita corrente.
 * <p>
 * Immutabile e thread-safe.
 */
public class SubmitProposalRequest extends Request{
    private final List<String> words;

    /**
     * Crea la richiesta incapsulando la sequenza di termini scelti dal giocatore.
     *
     * @param words lista contenente le parole proposte per la verifica del gruppo
     */
    public SubmitProposalRequest(List<String> words){
        super("submitProposal");
        this.words = words;
    }

    /** Restituisce la lista di parole inviate nella proposta. */
    public List<String> getWords(){
        return words;
    }
    
}