package common.payload;

import game.model.CardColor;

import java.io.Serial;
import java.io.Serializable;

public record PlayCardRequest(int handIndex, CardColor chosenColor) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
