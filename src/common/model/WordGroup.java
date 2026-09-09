package common.model;

import java.util.List;
import java.util.Collections;

/**
 * Modella un gruppo tematico composto da una categoria concettuale e dalle 4 parole associate.
 * <p>
 * Classe immutabile e thread-safe: la lista interna delle parole è incapsulata e non modificabile
 */
public class WordGroup {
    private final String theme;
    private final List<String> words;

    /**
     * Costruisce il gruppo tematico rendendo la lista delle parole immutabile.
     *
     * @param theme etichetta descrittiva o categoria comune
     * @param words lista dei 4 vocaboli costituenti il gruppo
     */
    public WordGroup(String theme, List<String> words){
        this.theme = theme;

        if(words != null){
            this.words = Collections.unmodifiableList(words);
        } else {
            this.words = Collections.emptyList();
        }
    }

    public String getTheme(){
        return theme;
    }

    /**
     * Restituisce la vista immutabile della lista di vocaboli del gruppo.
     * <p>
     * Query pura (nessun effetto collaterale).
     *
     * @return lista non modificabile dei vocaboli
     */
    public List<String> getWords(){
        if(this.words == null){
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(this.words);
    }
}