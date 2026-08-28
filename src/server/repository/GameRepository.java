package server.repository;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.model.GameRecord;

public class GameRepository {
    private final ConcurrentHashMap<Integer, GameRecord> games;
    private final String filePath;
    private final Gson gson;
    private final AtomicInteger idCounter;

    public GameRepository(String filePath){
        this.filePath = filePath;
        this.games = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.idCounter = new AtomicInteger();
    }
    
    public synchronized void loadFromDisk() throws IOException {
        Path path = Paths.get(this.filePath);
        if (!Files.exists(path)) { 
            return; 
        }

        try (FileReader reader = new FileReader(filePath, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<ConcurrentHashMap<Integer, GameRecord>>(){}.getType();
            ConcurrentHashMap<Integer, GameRecord> loadedGames = gson.fromJson(reader, type);
            if (loadedGames != null) {
                this.games.clear();
                this.games.putAll(loadedGames);

                int maxId = this.games.keySet().stream().max(Integer::compareTo).orElse(0);
                this.idCounter.set(maxId);
            }
        }
    }

    public synchronized void saveToDisk() throws IOException {
        Path path = Paths.get(filePath);
        
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(games, writer);
        }
    }

    public void addGameRecord(GameRecord record) {
        games.put(record.getGameId(), record);
    }
    
    public GameRecord getGameRecord(int gameId) {
        return games.get(gameId);
    }

    public int generateGameId(){
        return idCounter.incrementAndGet();
    }
}
