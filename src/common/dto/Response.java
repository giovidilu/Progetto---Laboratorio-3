package common.dto;

/**
 * Contenitore generico per le risposte applicative interne o di livello servizio.
 * <p>
 * Classe immutabile e thread-safe.
 */
public class Response {
    private final boolean success;
    private final String error;
    private final Object data;

    private Response(boolean success, String error, Object data){
        this.success = success;
        this.error = error;
        this.data = data;
    }

    /**
     * Crea un oggetto di esito positivo con allegato il dato di risposta.
     *
     * @param data payload del risultato
     * @return nuova istanza di {@link Response} con successo confermato
     */
    public static Response ok(Object data){
        return new Response(true, null, data);
    }

    /**
     * Crea un oggetto di esito positivo senza payload.
     *
     * @return nuova istanza di {@link Response}
     */
    public static Response ok(){
        return new Response(true, null, null);
    }

    /**
     * Crea un oggetto di risposta che segnala un fallimento applicativo.
     *
     * @param errorMessage descrizione dell'errore
     * @return nuova istanza di {@link Response} contenente l'errore
     */
    public static Response error(String errorMessage){
        return new Response(false, errorMessage, null);
    }

    /** Verifica se l'operazione ha avuto successo. */
    public boolean isSuccess(){ return success; }

    /** Restituisce l'eventuale messaggio di errore. */
    public String getError(){ return error; }

    /** Restituisce il payload allegato. */
    public Object getData(){ return data; }
}