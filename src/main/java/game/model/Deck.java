package game.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class Deck {
    private final Deque<Card> cards;

    public Deck() {
        List<Card> generated = new ArrayList<>();
        for (CardColor color : CardColor.values()) {
            if (color == CardColor.BLACK) {
                continue;
            }
            generated.add(Card.numberCard(color, 0));
            for (int number = 1; number <= 9; number++) {
                generated.add(Card.numberCard(color, number));
                generated.add(Card.numberCard(color, number));
            }
            generated.add(Card.actionCard(color, CardType.SKIP));
            generated.add(Card.actionCard(color, CardType.SKIP));
            generated.add(Card.actionCard(color, CardType.REVERSE));
            generated.add(Card.actionCard(color, CardType.REVERSE));
            generated.add(Card.actionCard(color, CardType.DRAW_TWO));
            generated.add(Card.actionCard(color, CardType.DRAW_TWO));
            generated.add(Card.actionCard(color, CardType.GROUP_DRAW));
            generated.add(Card.actionCard(color, CardType.TRIPLE_PEEK));
        }
        for (int index = 0; index < 4; index++) {
            generated.add(Card.wild(CardType.WILD));
            generated.add(Card.wild(CardType.WILD_DRAW_FOUR));
        }
        Collections.shuffle(generated);
        cards = new ArrayDeque<>(generated);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Deck is empty.");
        }
        return cards.removeFirst();
    }

    public Deque<Card> remainingCards() {
        return new ArrayDeque<>(cards);
    }
}
