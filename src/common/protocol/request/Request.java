package common.protocol.request;

/**
 * Radice della gerarchia delle richieste client trasmesse via TCP.
 * <p>
 * Definisce il campo obbligatorio {@code operation} imposto dal protocollo JSON di comunicazione.
 * Immutabile e thread-safe.
 */
public abstract class Request {
    private final String operation;

    public Request(String operation){
        this.operation = operation;
    }

    public String getOperation(){
        return  this.operation;
    }
}