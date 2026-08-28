package common.protocol.response.payload;

import java.util.List;

public class LeaderboardPayload {
    private final List<LeaderboardEntry> entries;

    public LeaderboardPayload(List<LeaderboardEntry> entries) {
        this.entries = entries;
    }

    public List<LeaderboardEntry> getEntries() {
        return entries;
    }
}