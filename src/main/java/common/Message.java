package common;

import java.io.Serial;
import java.io.Serializable;

public record Message(MessageType type, String playerId, Object payload) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
