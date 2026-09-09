package common.protocol.response;

/**
 * Codici di stato e di errore restituiti dal server nelle risposte di protocollo JSON.
 */
public enum ResponseCode {

    //Operazione elaborata con successo[cite: 43]. */
    SUCCESS,

    //Registrazione o aggiornamento falliti: username già occupato da un altro account
    USERNAME_ALREADY_TAKEN,
    //Autenticazione o verifica fallita: password errata per l'utente indicato
    INVALID_CREDENTIALS,
    //Operazione respinta: richiede che il client abbia effettuato il login
    NOT_LOGGED_IN,

    //Proposta rifiutata: formato non valido o parole già collocate/non pertinenti
    MALFORMED_PROPOSAL,
    //Identificativo partita specificato non presente negli archivi del server
    GAME_NOT_FOUND,
    //Richiesta classifica fallita: il giocatore specificato non esiste a sistema
    PLAYER_NOT_FOUND,

    //Messaggio JSON non conforme, parametri mancanti o operazione non riconosciuta
    BAD_REQUEST,
    //Condizione anomala imprevista durante l'elaborazione interna lato server. 
    INTERNAL_SERVER_ERROR;
}