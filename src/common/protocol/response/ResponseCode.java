package common.protocol.response;

public enum ResponseCode {

    SUCCESS, //Successo generale

    // Errori legati alla gestione dell'utente e autenticazione
    USERNAME_ALREADY_TAKEN, // Nome utente già registrato
    INVALID_CREDENTIALS,    // Password errata
    NOT_LOGGED_IN,          // Utente non logato

    // Errori legati alle macchine di gioco
    MALFORMED_PROPOSAL, // Proposta malformata
    GAME_NOT_FOUND,     // Gioco inesistente
    PLAYER_NOT_FOUND,   // Giocatore inesistente

    // Errori di validazione e di sistema
    BAD_REQUEST,            // Formato JSON o richiesta non validi
    INTERNAL_SERVER_ERROR; // Errore generico lato server
}