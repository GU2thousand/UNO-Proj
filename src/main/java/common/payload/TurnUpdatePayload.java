package common.payload;

import java.io.Serial;
import java.io.Serializable;

public record TurnUpdatePayload(String currentTurnPlayerId, String currentTurnUsername) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
