package common.payload;

import common.MatchSummary;
import common.PlayerStats;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record ConnectedPayload(String playerId, String username, PlayerStats stats, List<MatchSummary> recentMatches) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public ConnectedPayload {
        recentMatches = List.copyOf(recentMatches);
    }
}
