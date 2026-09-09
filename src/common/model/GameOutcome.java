package common.model;

/**
 * Esito finale della partita per un singolo giocatore.
 */
public enum GameOutcome {
    //Partita vinta individuando 3 gruppi corretti. 
    WON,
    //Partita persa per raggiungimento del limite di 4 errori consentiti. 
    LOST_BY_MISTAKES,
    //Partita conclusa senza risoluzione per scadenza del tempo globale di gioco. 
    DID_NOT_FINISH
}