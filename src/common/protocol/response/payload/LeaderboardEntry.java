package common.protocol.response.payload;

/**
 * Singola riga della classifica generale, che associa un giocatore alla sua posizione
 * e al punteggio complessivo cumulato.
 * <p>
 * Classe immutabile e thread-safe.
 */
public class LeaderboardEntry {
    private final int rank;
    private final String username;
    private final int score;

    /**
     * Costruisce una voce di classifica.
     *
     * @param rank posizione occupata (1-based)
     * @param username nome utente del giocatore
     * @param score punteggio complessivo
     */
    public LeaderboardEntry(int rank, String username, int score) {
        this.rank = rank;
        this.username = username;
        this.score = score;
    }

    public int getRank() { return rank; }
    public String getUsername() { return username; }
    public int getScore() { return score; }
}