package common.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


public class Game {
    private final GameTemplate gameTemplate;
    private final List<String> shuffledWords;
    private final long startTime;
    private final long endTime;

    public Game(GameTemplate gameTemplate, List<String> shuffledWords, long startTime, long endTime) {
        this.gameTemplate = gameTemplate;
        this.startTime = startTime;
        this.endTime = endTime;
        
        if (shuffledWords != null) {
            this.shuffledWords = Collections.unmodifiableList(new ArrayList<>(shuffledWords));
        } else {
            this.shuffledWords = Collections.emptyList();
        }
    }

    public Game(GameTemplate gameTemplate, long durationMillis) {
        this.gameTemplate = gameTemplate;
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime + durationMillis;

        List<String> tempWords = new ArrayList<>();
        // Usiamo il getter protetto di GameTemplate
        for (WordGroup group : this.gameTemplate.getGroups()) {
            tempWords.addAll(group.getWords());
        }
        Collections.shuffle(tempWords);
        
        this.shuffledWords = Collections.unmodifiableList(tempWords);
    }

    public GameTemplate getGameTemplate() {
        return gameTemplate;
    }

    public List<String> getShuffledWords() {
        // Protezione a runtime per i dati deserializzati da GSON
        if (this.shuffledWords == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.shuffledWords);
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
