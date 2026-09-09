package common.model;

/**
 * Esito aggregato conseguente all'invio di una proposta di parole da parte di un giocatore.
 * <p>
 * Raggruppa l'esito della mossa singola, l'eventuale esito conclusivo della partita,
 * il gruppo indovinato e lo snapshot aggiornato dello stato di gioco del giocatore.
 * Classe immutabile e thread-safe.
 */
public class ProposalResult {
    private final MoveOutcome moveOutcome;
    private final GameOutcome gameOutcome;
    private final WordGroup guessedGroup;
    private final PlayerGameState updatedState;

    /**
     * Costruisce il risultato dell'elaborazione di una proposta.
     *
     * @param moveOutcome esito della mossa singola (CORRECT, WRONG, MALFORMED, ecc.)
     * @param gameOutcome eventuale esito conclusivo del round (WON, LOST, o null se in corso)
     * @param guessedGroup eventuale gruppo tematico individuato con successo
     * @param updatedState snapshot dello stato aggiornato del giocatore
     */
    public ProposalResult(MoveOutcome moveOutcome, GameOutcome gameOutcome, WordGroup guessedGroup, PlayerGameState updatedState) {
        this.moveOutcome = moveOutcome;
        this.gameOutcome = gameOutcome;
        this.guessedGroup = guessedGroup;
        this.updatedState = updatedState;
    }

    /** Restituisce l'esito della specifica mossa effettuata. */
    public MoveOutcome getMoveOutcome() {
        return moveOutcome;
    }

    /** Restituisce l'eventuale esito terminale del round per l'utente. */
    public GameOutcome getGameOutcome() {
        return gameOutcome;
    }

    /** Restituisce il gruppo tematico indovinato con la proposta. */
    public WordGroup getGuessedGroup() {
        return guessedGroup;
    }

    /** Restituisce lo snapshot dello stato del giocatore conseguente alla mossa. */
    public PlayerGameState getUpdatedState() {
        return updatedState;
    }
}