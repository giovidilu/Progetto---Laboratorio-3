package server.service;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    public record UserSession(InetSocketAddress udpEndpoint) {}
    private final ConcurrentHashMap<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Effettua il login registrando l'endpoint UDP associato.
     *
     * @param username    Identificativo dell'utente.
     * @param udpEndpoint Indirizzo e porta UDP del client (può essere null).
     * @return true se il login ha successo, false se l'utente è già loggato.
     */
    public boolean login(String username, InetSocketAddress udpEndpoint) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return activeSessions.putIfAbsent(username, new UserSession(udpEndpoint)) == null;
    }

    /**
     * Sovraccarico per retrocompatibilità (login senza endpoint UDP).
     */
    public boolean login(String username) {
        return login(username, null);
    }

    public void logout(String username) {
        if (username != null) {
            activeSessions.remove(username);
        }
    }

    public boolean isLoggedIn(String username) {
        return username != null && activeSessions.containsKey(username);
    }

    /**
     * Restituisce l'endpoint UDP associato alla sessione dell'utente.
     *
     * @param username Nome utente.
     * @return L'oggetto InetSocketAddress o null se non presente o utente non loggato.
     */
    public InetSocketAddress getUdpEndpoint(String username) {
        if (username == null) {
            return null;
        }
        UserSession session = activeSessions.get(username);
        return (session != null) ? session.udpEndpoint() : null;
    }

    public void clear() {
        activeSessions.clear();
    }

}
