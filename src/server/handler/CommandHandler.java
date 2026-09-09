package server.handler;

import com.google.gson.JsonObject;
import common.protocol.response.ServerResponse;

/**
 * Interfaccia funzionale per l'incapsulamento della logica di gestione di un singolo comando.
 * <p>
 * Implementata tramite il pattern Command/Dispatching da {@link ClientHandler} per associare
 * le operazioni di protocollo ai rispettivi metodi di esecuzione.
 */
@FunctionalInterface
public interface CommandHandler {

    /**
     * Elabora il comando incapsulato nella richiesta JSON e genera la relativa risposta.
     *
     * @param request payload della richiesta in formato {@link JsonObject}
     * @return risposta tipizzata pronta per la serializzazione e la trasmissione al client
     */
    ServerResponse<?> handle(JsonObject request);
}