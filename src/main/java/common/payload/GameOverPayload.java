package common.payload;

import java.io.Serial;
import java.io.Serializable;

public record GameOverPayload(String winnerPlayerId, String winnerUsername) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
