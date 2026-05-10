package common;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record RoomSnapshot(
        String roomId,
        String hostPlayerId,
        int maxPlayers,
        boolean started,
        List<PlayerInfo> players
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
