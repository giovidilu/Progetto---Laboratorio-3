package common.model;

import java.util.List;
import java.util.Collections;

public class WordGroup {
    private final String theme;
    private final List<String> words;

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

    public List<String> getWords(){
        if(this.words == null){
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(this.words);
    }
}
