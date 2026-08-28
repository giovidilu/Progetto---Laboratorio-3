package common.dto;

import java.util.HashSet;
import java.util.List;

public class SubmitProposalRequest {
    private List<String> words;

    public SubmitProposalRequest() {
    }

    public SubmitProposalRequest(List<String> words) {
        this.words = words;
    }

    public List<String> getWords() {
        return words;
    }

    public boolean isValid(){
        if(words == null || words.size() != 4){
            return false;
        }

        for(String word: words){
            if(word == null || word.isBlank()){
                return false;
            }
        }

        return new HashSet<>(words).size() == 4;
    }
}
