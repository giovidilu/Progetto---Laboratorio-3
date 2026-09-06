package common.protocol.request;

import common.protocol.OperationType;

public class LoginRequest extends CredentialsRequest {
    private final Integer udpPort;

    public LoginRequest(String username, String psw, Integer udpPort) {
        super(OperationType.LOGIN, username, psw);
        this.udpPort = udpPort;
    }

    public LoginRequest(String username, String psw) {
        this(username, psw, null);
    }

    public Integer getUdpPort() {
        return udpPort;
    }
}