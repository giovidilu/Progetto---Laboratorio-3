package common.protocol.response;

public class ServerResponse<T> {

    private final ResponseCode status;
    private final String message;
    private final T payload;


    
    private ServerResponse(ResponseCode status, String message, T payload){
        this.status = status;
        this.message = message;
        this.payload = payload;
    }

    //Costruttore 1: Operazione completata con successo che restituisce un payload
    public static <T>ServerResponse<T> successWithPayload(ResponseCode status, T payload){
        return new ServerResponse<>(status,null,payload);
    }

    //Costruttore 2: Operazione Fallita senza alcun payload di dati
    public static <T> ServerResponse<T> failWithMessage(ResponseCode status, String message){
        return new ServerResponse<>(status,message,null);
    }

    //Costruttore 3: Operazione completata senza payload e senza messaggi
    public  static <T> ServerResponse<T> successWithoutPayload(ResponseCode status){
        return new ServerResponse<>(status,null,null);
    }

    public ResponseCode getStatus(){
        return status;
    }

    public String getMessage(){
        return message;
    }

    public T getPayload(){
        return payload;
    }

}
