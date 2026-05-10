package common;

import java.io.Serial;
import java.io.Serializable;

public record PlayerInfo(String playerId, String username, int cardCount, boolean bot) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
