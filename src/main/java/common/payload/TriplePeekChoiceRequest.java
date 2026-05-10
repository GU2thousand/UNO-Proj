package common.payload;

import java.io.Serial;
import java.io.Serializable;

public record TriplePeekChoiceRequest(int optionIndex) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
