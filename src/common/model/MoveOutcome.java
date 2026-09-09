package common.model;

/**
 * Risultato della valutazione di una proposta di raggruppamento inviata dal giocatore.
 */
public enum MoveOutcome {
    /** La proposta corrisponde a uno dei gruppi tematici validi. */
    CORRECT,
    /** La proposta non forma un gruppo valido (incrementa il conteggio degli errori). */
    WRONG,
    /** La proposta contiene parole non valide o già risolte (nessun errore addebitato). */
    MALFORMED,
    /** La mossa è stata rifiutata poiché la partita per l'utente è già conclusa. */
    ALREADY_COMPLETED;
}