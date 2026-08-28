package common.protocol.response.payload;

import common.protocol.response.GameState;
import java.util.List;

public class GameInfoPayload {

    private final GameState state;

    // Campi condivisi tra partita in corso e conclusa
    private final Integer errors;
    private final Integer score;

    // Campi specifici per partita in corso
    private final Integer timeRemaining;
    private final List<List<String>> correctGroups;
    private final List<String> words;

    // Campi specifici per partita conclusa
    private final List<List<String>> finalAllocations;
    private final Integer numberCorrectGroups;

    private GameInfoPayload(GameState state, Integer errors, Integer  score,
                            Integer  timeRemaining, List<String> words,
                            List<List<String>> correctGroups, List<List<String>> finalAllocations,
                            Integer  numberCorrectGroups) {
        this.state = state;
        this.errors = errors;
        this.score = score;
        this.timeRemaining = timeRemaining;
        this.words = words;
        this.correctGroups = correctGroups;
        this.finalAllocations = finalAllocations;
        this.numberCorrectGroups = numberCorrectGroups;
    }

    public static GameInfoPayload OngoingGame(Integer timeRemaining, 
                                              List<List<String>> correctGroups, 
                                              List<String> words, 
                                              Integer errors, 
                                              Integer score){
        return  new GameInfoPayload(GameState.ONGOING, errors, score, timeRemaining, words, correctGroups, null, null); 
    }

    public static GameInfoPayload FinishedGame(List<List<String>> finalAllocations, 
                                               Integer numberCorrectGroups, 
                                               Integer errors, 
                                               Integer score){
        return  new GameInfoPayload(GameState.FINISHED, errors, score, null, null, null, finalAllocations, numberCorrectGroups); 
    }

    public GameState getState(){
        return state;
    }

    public Integer getErrors(){
        return errors;
    }

    public Integer getScore(){
        return score;
    }

    public Integer getTimeRemaining(){
        return timeRemaining;
    }

    public List<List<String>> getCorrectGroups() {
        return correctGroups;
    }

    public List<String> getWords() {
        return words;
    }

    public List<List<String>> getFinalAllocation() {
        return finalAllocations;
    }

    public Integer getNumberCorrectGroups() {
        return numberCorrectGroups;
    }

}
