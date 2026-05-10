package game.model;

import java.io.Serial;
import java.io.Serializable;

public record Card(CardColor color, CardType type, int number) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static Card numberCard(CardColor color, int number) {
        return new Card(color, CardType.NUMBER, number);
    }

    public static Card actionCard(CardColor color, CardType type) {
        return new Card(color, type, -1);
    }

    public static Card wild(CardType type) {
        return new Card(CardColor.BLACK, type, -1);
    }

    public String displayLabel() {
        return switch (type) {
            case NUMBER -> Integer.toString(number);
            case SKIP -> "Skip";
            case REVERSE -> "Reverse";
            case DRAW_TWO -> "+2";
            case WILD -> "Wild";
            case WILD_DRAW_FOUR -> "W+4";
            case GROUP_DRAW -> "Group";
            case TRIPLE_PEEK -> "Peek";
        };
    }

    @Override
    public String toString() {
        return color.displayName() + " " + displayLabel();
    }
}
