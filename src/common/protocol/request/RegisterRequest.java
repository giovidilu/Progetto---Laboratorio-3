package common.protocol.request;

public class RegisterRequest extends CredentialsRequest {
    public RegisterRequest(String username, String psw) {
        super("register", username, psw);
    }
}

