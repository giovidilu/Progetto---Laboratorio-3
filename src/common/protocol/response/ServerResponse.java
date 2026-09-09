package common.protocol.response;

/**
 * Formato standard per tutti i messaggi di risposta inviati dal server ai client via TCP.
 * <p>
 * Trasporta un codice di stato {@link ResponseCode}, un messaggio testuale opzionale
 * e un payload generico tipizzato {@code T}.
 * Classe immutabile e thread-safe.
 *
 * @param <T> tipo del payload associato alla risposta
 */
public class ServerResponse<T> {

    private final ResponseCode status;
    private final String message;
    private final T payload;

    /**
     * Costruttore privato; l'istanziazione avviene tramite factory method statici.
     *
     * @param status codice di stato o errore
     * @param message messaggio descrittivo opzionale
     * @param payload dati associati alla risposta
     */
    private ServerResponse(ResponseCode status, String message, T payload){
        this.status = status;
        this.message = message;
        this.payload = payload;
    }

    /**
     * Costruisce una risposta corredata dal payload dei dati richiesti.
     *
     * @param <T> tipo del payload
     * @param status codice di esito positivo
     * @param payload dati del risultato applicativo
     * @return nuova istanza di {@link ServerResponse}
     */
    public static <T>ServerResponse<T> successWithPayload(ResponseCode status, T payload){
        return new ServerResponse<>(status,null,payload);
    }

    /**
     * Costruisce una risposta di errore priva di payload, ma con una descrizione diagnostica.
     *
     * @param <T> tipo del payload
     * @param status codice di errore specifico
     * @param message dettaglio testuale della causa del fallimento
     * @return nuova istanza di {@link ServerResponse}
     */
    public static <T> ServerResponse<T> failWithMessage(ResponseCode status, String message){
        return new ServerResponse<>(status,message,null);
    }

    /**
     * Costruisce una risposta di successo per operazioni che non restituiscono dati aggiuntivi.
     *
     * @param <T> tipo del payload
     * @param status codice di esito positivo
     * @return nuova istanza di {@link ServerResponse} priva di payload e messaggio
     */
    public  static <T> ServerResponse<T> successWithoutPayload(ResponseCode status){
        return new ServerResponse<>(status,null,null);
    }

    /** Restituisce il codice di stato della risposta. */
    public ResponseCode getStatus(){
        return status;
    }

    /** Restituisce l'eventuale messaggio di dettaglio o errore associato. */
    public String getMessage(){
        return message;
    }

    /** Restituisce il payload allegato alla risposta. */
    public T getPayload(){
        return payload;
    }

}