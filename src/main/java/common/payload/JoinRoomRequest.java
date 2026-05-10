package common.payload;

import java.io.Serial;
import java.io.Serializable;

public record JoinRoomRequest(String roomId) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
