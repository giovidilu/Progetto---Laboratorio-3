package common.protocol.request;


public abstract class Request {
    private final String operation;

    public Request(String operation){
        this.operation = operation;
    }

    public String getOperation(){
        return  this.operation;
    }
}
