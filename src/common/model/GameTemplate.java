package common.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class GameTemplate {
    
    private final int gameId;
    private final List<WordGroup> groups;

    public GameTemplate(int gameId, List<WordGroup> groups) {
        this.gameId = gameId;

        if (groups != null) {
            this.groups = Collections.unmodifiableList(new ArrayList<>(groups));
        } else {
            this.groups = Collections.emptyList();
        }
    }

    public int getGameId() {
        return gameId;
    }

    public List<WordGroup> getGroups() {
        
        if (this.groups == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.groups);
    }
}