package common.payload;

import game.model.Card;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record TriplePeekOptionsPayload(List<Card> options) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public TriplePeekOptionsPayload {
        options = List.copyOf(options);
    }
}
