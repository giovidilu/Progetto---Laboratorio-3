package common.dto;

public class UpdateCredentialsRequest {
    private String oldUsername;
    private String oldPsw;
    private String newUsername;
    private String newPsw;

    public UpdateCredentialsRequest(){}

    public UpdateCredentialsRequest(String oldUsername, String oldPsw, String newUsername, String newPsw){
        this.oldUsername = oldUsername;
        this.oldPsw = oldPsw;
        this.newUsername = newUsername;
        this.newPsw = newPsw;
    }

    public String getOldUsername(){
        return oldUsername;
    }

    public String getOldPsw(){
        return oldPsw;
    }

    public String getNewUsername(){
        return newUsername;
    }

    public String getNewPsw(){
        return newPsw;
    }

    public boolean isValid(){
        if(oldUsername == null || oldUsername.isBlank() || oldPsw == null || oldPsw.isBlank()){
            return false;
        }

        boolean hasValidNewUser = newUsername != null && !newUsername.isBlank();
        boolean hasValidNewPsw = newPsw != null && !newPsw.isBlank();

        if(newUsername != null && newUsername.isBlank()){
            return false;
        }
        if(newPsw != null && newPsw.isBlank()){
            return false;
        }
        return hasValidNewUser || hasValidNewPsw;

    }


}
