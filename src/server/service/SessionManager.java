package server.service;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestore concorrente delle sessioni di autenticazione e del recapito UDP dei client connessi.
 * <p>
 * Previene accessi concorrenti multipli con lo stesso account e mappa gli endpoint UDP associati
 * ai client per la ricezione asincrona delle notifiche.
 * Thread-safe: incapsula lo stato in una {@link ConcurrentHashMap}.
 */
public class SessionManager {

    /**
     * Descrittore immutabile della sessione attiva di un client.
     */
    public record UserSession(InetSocketAddress udpEndpoint) {}
    private final ConcurrentHashMap<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Registra la sessione dell'utente associandola facoltativamente a un endpoint UDP.
     * <p>
     * Metodo con effetto collaterale atomico: rifiuta la registrazione se l'utente risulta già autenticato.
     *
     * @param username identificativo univoco dell'utente
     * @param udpEndpoint indirizzo di recapito per i datagrammi UDP (può essere {@code null})
     * @return {@code true} se la sessione è stata registrata con successo, {@code false} se l'utente è già loggato
     */
    public boolean login(String username, InetSocketAddress udpEndpoint) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return activeSessions.putIfAbsent(username, new UserSession(udpEndpoint)) == null;
    }

    /**
     * Sovraccarico per il login senza notifica UDP.
     */
    public boolean login(String username) {
        return login(username, null);
    }

    /**
     * Rimuove la sessione attiva per l'utente specificato.
     * <p>
     * Metodo con effetto collaterale atomico e thread-safe.
     *
     * @param username identificativo dell'utente da disconnettere
     */
    public void logout(String username) {
        if (username != null) {
            activeSessions.remove(username);
        }
    }

    /**
     * Verifica se un determinato account è attualmente autenticato a sistema.
     * <p>
     * Query pura e lock-free.
     *
     * @param username identificativo dell'utente
     * @return {@code true} se presente nelle sessioni attive, {@code false} altrimenti
     */
    public boolean isLoggedIn(String username) {
        return username != null && activeSessions.containsKey(username);
    }

    /**
     * Recupera l'indirizzo socket UDP registrato per l'utente autenticato.
     * <p>
     * Query pura e lock-free.
     *
     * @param username identificativo dell'utente
     * @return {@link InetSocketAddress} configurato, oppure {@code null} se non presente o privo di porta UDP
     */
    public InetSocketAddress getUdpEndpoint(String username) {
        if (username == null) {
            return null;
        }
        UserSession session = activeSessions.get(username);
        return (session != null) ? session.udpEndpoint() : null;
    }

    /**
     * Restituisce una fotografia di tutti gli endpoint UDP validi appartenenti agli utenti al momento connessi.
     * <p>
     * Query pura e thread-safe.
     *
     * @return mappa delle destinazioni UDP indicizzata per username
     */
    public Map<String, InetSocketAddress> getActiveUdpEndpoints() {
        Map<String, InetSocketAddress> endpoints = new HashMap<>();
        activeSessions.forEach((user, session) -> {
            if (session != null && session.udpEndpoint() != null) {
                endpoints.put(user, session.udpEndpoint());
            }
        });
        return endpoints;
    }

    /**
     * Invalida e rimuove tutte le sessioni correntemente attive.
     */
    public void clear() {
        activeSessions.clear();
    }
}