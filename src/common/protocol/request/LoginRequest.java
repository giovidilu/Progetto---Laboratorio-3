package common.protocol.request;

public class LoginRequest extends CredentialsRequest {
    public LoginRequest(String username, String psw) {
        super("login", username, psw);
    }
}
