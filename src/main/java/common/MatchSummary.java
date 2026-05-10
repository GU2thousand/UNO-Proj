package common;

import java.io.Serial;
import java.io.Serializable;

public record MatchSummary(long matchId, String winnerName, String playedAt, int playerCount, String result) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
