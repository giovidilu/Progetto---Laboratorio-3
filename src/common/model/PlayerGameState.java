package common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerGameState {
    private final String username;
    private final int gameId;
    private final List<WordGroup> correctGroups;
    private int mistakes;

    public PlayerGameState(String username, int gameId){
        this.username = username;
        this.gameId = gameId;
        this.correctGroups = new ArrayList<>();
        this.mistakes = 0;
    }

    public String getUsername(){
        return username;
    }

    public int getGameId(){
        return gameId;
    }

    public List<WordGroup> getCorrectGroups(){
        return Collections.unmodifiableList(correctGroups);
    }

    public int getMistakes(){
        return mistakes;
    }

    public void addCorrectGroup(WordGroup group){
        if(group != null && !this.correctGroups.contains(group)){
            this.correctGroups.add(group);
        }
    }

    public void incrementMistakes() {
        if (this.mistakes >= 4) {
            throw new IllegalStateException("Raggiunto il limite massimo di 4 errori consentiti.");
        }
        this.mistakes++;
    }

    public int getScore(){
        int pointsFromCorrect = this.correctGroups.size() * 6;
        int penaltyFromMistakes = this.mistakes * 4;
        return pointsFromCorrect - penaltyFromMistakes;
    }

    public GameOutcome getOutcome(){
        if (this.correctGroups != null && this.correctGroups.size() >= 3) {
            return GameOutcome.WON;
        }
        if(this.mistakes >= 4){
            return GameOutcome.LOST_BY_MISTAKES;
        }

        return null;
    }
}
