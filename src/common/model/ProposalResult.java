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

    public ProposalResult(MoveOutcome moveOutcome, GameOutcome gameOutcome, WordGroup guessedGroup, PlayerGameState updatedState) {
        this.moveOutcome = moveOutcome;
        this.gameOutcome = gameOutcome;
        this.guessedGroup = guessedGroup;
        this.updatedState = updatedState;
    }

    public MoveOutcome getMoveOutcome() {
        return moveOutcome;
    }

    public GameOutcome getGameOutcome() {
        return gameOutcome;
    }

    public WordGroup getGuessedGroup() {
        return guessedGroup;
    }

    public PlayerGameState getUpdatedState() {
        return updatedState;
    }
}