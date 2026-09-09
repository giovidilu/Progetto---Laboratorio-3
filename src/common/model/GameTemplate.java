package common.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Modello descrittivo statico di una configurazione di puzzle.
 * <p>
 * Definisce l'identificativo del puzzle e i 4 gruppi tematici di parole associati.
 * Classe immutabile e thread-safe.
 */
public class GameTemplate {
    
    private final int gameId;
    private final List<WordGroup> groups;

    /**
     * Costruisce il template incapsulando la lista dei gruppi in una collezione non modificabile.
     *
     * @param gameId identificativo univoco del template di gioco
     * @param groups lista dei gruppi che compongono il puzzle
     */
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