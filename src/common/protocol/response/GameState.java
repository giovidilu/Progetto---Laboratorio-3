package common.protocol.response;

/**
 * Stato temporale e operativo di una partita di gioco
 */
public enum GameState {
    //Partita attiva in fase di svolgimento entro la finestra temporale
    ONGOING,
    //Partita terminata per decorrenza del tempo limite o interruzione globale
    FINISHED;
}