package common.protocol.response.payload;

import java.util.List;

public class LoginPayload {
    private final List<String> words;
    private final List<List<String>> correctGroups;
    private final int errors;
    private final int timeRemaining;
    private final int score;

    public LoginPayload(List<String> words, List<List<String>> correctGroups, int errors, int timeRemaining, int score){
        this.words = words;
        this.correctGroups = correctGroups;
        this.errors = errors;
        this.timeRemaining = timeRemaining;
        this.score = score;
    }

    public List<String> getWords(){
        return words;
    }

    public List<List<String>> getCorrectGroups(){
        return correctGroups;
    }
    public int getErrors(){
        return errors;
    }
    public int getTimeRemaining(){
        return timeRemaining;
    }
    public int getScore(){
        return score;
    }
}
