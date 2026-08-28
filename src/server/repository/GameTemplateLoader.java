package server.repository;

import common.model.GameTemplate;
import common.model.WordGroup;

import com.google.gson.stream.JsonReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class GameTemplateLoader {

    private WordGroup readGroup(JsonReader reader) throws IOException {
    String theme = null;
    List<String> words = new ArrayList<>();

    reader.beginObject();
    
    while (reader.hasNext()) {
        String name = reader.nextName();
        switch (name) {
            case "theme":
                theme = reader.nextString();
                break;
            case "words":
                reader.beginArray();
                while (reader.hasNext()) {
                    words.add(reader.nextString());
                } 
                reader.endArray();
                break;
            default:
                reader.skipValue();
                break;
        } 
    } 
    
    reader.endObject();
    
    return new WordGroup(theme, words);
} 
    
    public Map<Integer, GameTemplate> loadTemplates(String filePath) throws IOException{
        Map<Integer, GameTemplate> templates = new HashMap<>();

        try(JsonReader reader = new JsonReader(new FileReader(filePath))){
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();

                int gameId = -1;
                List<WordGroup> groups = new ArrayList<>();
                
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    switch (name) {
                        case "gameId":
                            gameId = reader.nextInt();
                            break;
                        case "groups":
                            reader.beginArray();
                            while (reader.hasNext()) {
                                groups.add(readGroup(reader));                            
                            }
                            reader.endArray();
                            break;
                    
                        default:
                            reader.skipValue();
                            break;
                    }
                }
                reader.endObject();

                if(gameId != -1 && !groups.isEmpty()){
                    templates.put(gameId, new GameTemplate(gameId,groups));
                }
            }


            reader.endArray();
        }
        return templates;
    }
}
