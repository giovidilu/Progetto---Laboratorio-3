package common.model;

public class User {
    private String username;
    private String passwordHash;
    private String salt;
    private UserStats stats;

    public User(){
        this.stats = new UserStats();
    }

    public User(String username, String passwordHash, String salt){
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.stats = new UserStats();
    }

    public String getUsername(){
        return username;
    }
    public void setUsername(String username){
        this.username = username;
    }

    public String getPasswordHash(){
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public String getSalt(){
        return salt;
    }
    public void setSalt(String salt){
        this.salt = salt;
    }

    public UserStats getStats(){
        if(this.stats ==  null){
            this.stats = new UserStats();
        } 
        return this.stats;
    }

    public void setStats(UserStats stats){
        this.stats = stats;
    }

}
