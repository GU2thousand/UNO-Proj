package common;

import java.io.Serial;
import java.io.Serializable;

public record PlayerStats(String username, int gamesPlayed, int wins, double winRate) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
