package common.payload;

import java.io.Serial;
import java.io.Serializable;

public record UpdateMaxPlayersRequest(int maxPlayers) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
