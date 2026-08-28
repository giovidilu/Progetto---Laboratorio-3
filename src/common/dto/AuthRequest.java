package common.dto;

public class AuthRequest {
    private String username;
    private String psw;

    public String getUsername(){ return username; }
    public String getPsw(){ return psw; }

    public boolean isValid(){
        return username != null && !username.isBlank() && psw != null && !psw.isBlank();
    }
}
