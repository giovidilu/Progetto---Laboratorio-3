package common.protocol.request;

/**
 * Radice della gerarchia delle richieste client trasmesse via TCP.
 * <p>
 * Definisce il campo obbligatorio {@code operation} imposto dal protocollo JSON di comunicazione.
 * Immutabile e thread-safe.
 */
public abstract class Request {
    private final String operation;

    /**
     * Inizializza la richiesta con l'operazione identificativa di protocollo.
     *
     * @param operation nome del comando inviato
     */
    public Request(String operation){
        this.operation = operation;
    }

    /** Restituisce il nome dell'operazione di protocollo. */
    public String getOperation(){
        return  this.operation;
    }
}