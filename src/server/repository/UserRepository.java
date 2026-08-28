package server.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import common.model.User;

public class UserRepository {
    private final ConcurrentHashMap<String, User> users;
    private final String filePath;
    private final Gson gson;

    public UserRepository(String filePath){
        this.filePath = filePath;
        this.users = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();    
    }

    public synchronized void loadFromDisk() throws IOException {
        Path path = Paths.get(this.filePath);
        if(!Files.exists(path)){ 
            return;
        }

        try (FileReader reader = new FileReader(filePath, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<ConcurrentHashMap<String, User>>(){}.getType();
            ConcurrentHashMap<String, User> loadedUsers = gson.fromJson(reader, type);
            if (loadedUsers != null) {
                this.users.clear();
                this.users.putAll(loadedUsers);
            }
        }
    }

    public synchronized void saveToDisk() throws IOException {
        Path path = Paths.get(filePath);
        
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(users, writer);
        }
    }

    public synchronized boolean addUser(User user) {
        User previous = users.putIfAbsent(user.getUsername(), user);
        return previous == null;
    }

    public synchronized User getUser(String username) {
        return users.get(username);
    }

    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized boolean updateCredentials(String oldUsername, String newUsername, String newPasswordHash, String newSalt){
        User user = users.get(oldUsername);
        if(user == null ){
            return false;
        }

        if(newUsername != null && !newUsername.equals(oldUsername)){
            if (users.containsKey(newUsername)) {
                return false;
            }

            users.remove(oldUsername);
            user.setUsername(newUsername);

            if(newPasswordHash != null && newSalt != null){
                user.setPasswordHash(newPasswordHash);
                user.setSalt(newSalt);
            }

            users.put(newUsername, user);
            return true;
        }

        if(newPasswordHash != null && newSalt != null){
            user.setPasswordHash(newPasswordHash);
            user.setSalt(newSalt);
        }

        return true;
    }
}
