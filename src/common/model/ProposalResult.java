package common.model;

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
