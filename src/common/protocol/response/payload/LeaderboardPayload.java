package common.protocol.response.payload;

import java.util.List;

/**
 * Payload di risposta per l'operazione {@code requestLeaderboard}, contenente l'elenco
 * ordinato delle voci di classifica richieste.
 * <p>
 * Classe immutabile e thread-safe.
 */
public class LeaderboardPayload {
    private final List<LeaderboardEntry> entries;

    /**
     * Costruisce il payload incapsulando la lista delle voci di classifica.
     *
     * @param entries lista ordinata di {@link LeaderboardEntry} da trasmettere al client
     */
    public LeaderboardPayload(List<LeaderboardEntry> entries) {
        this.entries = entries;
    }

    /** Restituisce la lista delle voci che compongono la classifica. */
    public List<LeaderboardEntry> getEntries() {
        return entries;
    }
}