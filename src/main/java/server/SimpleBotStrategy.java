package server;

import game.logic.RuleEngine;
import game.model.Card;
import game.model.CardColor;
import game.model.GameState;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SimpleBotStrategy {
    public int chooseFirstPlayableCard(List<Integer> playableIndexes) {
        if (playableIndexes.isEmpty()) {
            throw new IllegalArgumentException("No playable cards were provided.");
        }
        return playableIndexes.get(0);
    }

    public CardColor chooseColor(List<Card> hand, int playedCardIndex) {
        Map<CardColor, Integer> colorCounts = new EnumMap<>(CardColor.class);
        for (int index = 0; index < hand.size(); index++) {
            if (index == playedCardIndex) {
                continue;
            }

            Card card = hand.get(index);
            if (card.color() == CardColor.BLACK) {
                continue;
            }
            colorCounts.merge(card.color(), 1, Integer::sum);
        }

        return colorCounts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(CardColor.RED);
    }

    public boolean shouldChallengeWildDrawFour() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    public int chooseTriplePeekOption(List<Card> options, RuleEngine ruleEngine, GameState state) {
        for (int index = 0; index < options.size(); index++) {
            if (ruleEngine.canPlayByActiveColorOrTop(options.get(index), state)) {
                return index;
            }
        }
        return 0;
    }
}
