package game.logic;

import game.model.Card;
import game.model.CardColor;
import game.model.CardType;
import game.model.GameState;
import game.model.PlayerState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleEngine {
    public boolean canPlay(Card card, GameState state) {
        if (state.isWildDrawFourChallengePending() || state.isTriplePeekChoicePending()) {
            return false;
        }
        if (card.type() == CardType.GROUP_DRAW || card.type() == CardType.TRIPLE_PEEK) {
            return false;
        }
        if (state.getPendingDrawCount() > 0) {
            return card.type() == CardType.DRAW_TWO;
        }

        return canPlayByActiveColorOrTop(card, state);
    }

    public boolean canPlayByActiveColorOrTop(Card card, GameState state) {
        if (card.type() == CardType.GROUP_DRAW || card.type() == CardType.TRIPLE_PEEK) {
            return false;
        }

        Card topDiscard = state.getTopDiscard();
        CardColor activeColor = state.getCurrentColor();
        if (topDiscard == null || activeColor == null) {
            return true;
        }

        if (card.type() == CardType.WILD || card.type() == CardType.WILD_DRAW_FOUR) {
            return true;
        }
        if (card.color() == activeColor) {
            return true;
        }
        if (card.type() == CardType.NUMBER && topDiscard.type() == CardType.NUMBER) {
            return card.number() == topDiscard.number();
        }
        return card.type() == topDiscard.type();
    }

    public boolean hasStandardPlayableCard(GameState state, PlayerState player) {
        return !getStandardPlayableCardIndexes(state, player).isEmpty();
    }

    public boolean canDraw(GameState state, PlayerState player) {
        if (state.isFinished() || state.isWildDrawFourChallengePending() || state.isTriplePeekChoicePending()) {
            return false;
        }
        if (state.getPendingDrawCount() > 0) {
            return true;
        }
        return !state.hasCurrentPlayerDrawn() && !hasStandardPlayableCard(state, player);
    }

    public List<Integer> getPlayableCardIndexes(GameState state, PlayerState player) {
        if (state.isFinished() || state.isTriplePeekChoicePending()) {
            return List.of();
        }

        List<Integer> standardPlayable = getStandardPlayableCardIndexes(state, player);
        if (state.getPendingDrawCount() > 0) {
            return Collections.unmodifiableList(standardPlayable);
        }

        if (state.hasCurrentPlayerDrawn()) {
            int drawnCardIndex = state.getDrawnCardIndexThisTurn();
            if (drawnCardIndex >= 0 && standardPlayable.contains(drawnCardIndex)) {
                return List.of(drawnCardIndex);
            }
            return List.of();
        }

        if (!standardPlayable.isEmpty()) {
            return Collections.unmodifiableList(standardPlayable);
        }
        return Collections.unmodifiableList(getCustomActionCardIndexes(player));
    }

    public boolean playCard(GameState state, String playerId, int handIndex, CardColor chosenColor) {
        PlayerState player = requireCurrentPlayer(state, playerId);
        validateHandIndex(player, handIndex);

        List<Integer> playableCardIndexes = getPlayableCardIndexes(state, player);
        if (!playableCardIndexes.contains(handIndex)) {
            throw new InvalidMoveException("That card cannot be played right now.");
        }

        Card selectedCard = player.getCardAt(handIndex);
        if (selectedCard.type() == CardType.GROUP_DRAW) {
            return playGroupDraw(state, player, handIndex);
        }
        if (selectedCard.type() == CardType.TRIPLE_PEEK) {
            return playTriplePeek(state, player, handIndex);
        }

        boolean wildDrawFourLegal = selectedCard.type() != CardType.WILD_DRAW_FOUR
                || isWildDrawFourLegal(state, player, handIndex);

        Card playedCard = player.removeCardAt(handIndex);
        state.getDiscardPile().push(playedCard);
        state.resetTurnDrawState();
        resolveActionEffect(state, player, playedCard, chosenColor, wildDrawFourLegal);

        if (player.handSize() == 0 && !state.isWildDrawFourChallengePending()) {
            state.markWinner(player);
            return false;
        }
        if (player.handSize() == 0 && state.isWildDrawFourChallengePending()) {
            state.setPendingWinner(player.getPlayerId(), player.getUsername());
        }

        return true;
    }

    public boolean chooseTriplePeekOption(GameState state, String playerId, int optionIndex) {
        PlayerState player = requireCurrentPlayer(state, playerId);
        if (!state.isTriplePeekChoicePending()) {
            throw new InvalidMoveException("There is no Triple Peek choice pending.");
        }
        if (!playerId.equals(state.getTriplePeekPlayerId())) {
            throw new InvalidMoveException("Only the active Triple Peek player can choose a card.");
        }

        List<Card> options = new ArrayList<>(state.getTriplePeekOptions());
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new InvalidMoveException("Choose one of the available Triple Peek cards.");
        }

        Card chosenCard = options.remove(optionIndex);
        player.addCard(chosenCard);
        for (Card remaining : options) {
            state.getDrawPile().addLast(remaining);
        }
        state.clearTriplePeekChoice();

        if (canPlay(chosenCard, state)) {
            return autoPlayAcquiredCard(state, player, player.handSize() - 1);
        }

        state.resetTurnDrawState();
        advanceTurn(state, 1);
        return true;
    }

    public boolean drawCard(GameState state, String playerId) {
        PlayerState player = requireCurrentPlayer(state, playerId);

        if (state.isWildDrawFourChallengePending()) {
            throw new InvalidMoveException("Respond to the Wild Draw Four by accepting or challenging it.");
        }
        if (state.isTriplePeekChoicePending()) {
            throw new InvalidMoveException("Choose one of the Triple Peek cards before taking another action.");
        }

        if (state.getPendingDrawCount() > 0) {
            int cardsToDraw = state.getPendingDrawCount();
            drawCards(state, player, cardsToDraw);
            state.setPendingDrawCount(0);
            advanceTurn(state, 1);
            return true;
        }

        if (state.hasCurrentPlayerDrawn()) {
            throw new InvalidMoveException("You have already drawn this turn.");
        }

        if (hasStandardPlayableCard(state, player)) {
            throw new InvalidMoveException("You can only draw when you have no playable ordinary cards.");
        }

        refillDrawPileIfNeeded(state, 1);
        if (state.getDrawPile().isEmpty()) {
            throw new InvalidMoveException("No cards are available to draw.");
        }

        Card drawnCard = state.getDrawPile().removeFirst();
        player.addCard(drawnCard);
        int drawnCardIndex = player.handSize() - 1;

        if (canPlay(drawnCard, state)) {
            return autoPlayAcquiredCard(state, player, drawnCardIndex);
        }

        state.resetTurnDrawState();
        advanceTurn(state, 1);
        return true;
    }

    public boolean endTurn(GameState state, String playerId) {
        requireCurrentPlayer(state, playerId);
        if (state.isWildDrawFourChallengePending()) {
            throw new InvalidMoveException("Respond to the Wild Draw Four by accepting or challenging it.");
        }
        if (state.isTriplePeekChoicePending()) {
            throw new InvalidMoveException("Choose one of the Triple Peek cards before ending your turn.");
        }
        if (!state.hasCurrentPlayerDrawn()) {
            throw new InvalidMoveException("You can only end your turn after drawing a playable card.");
        }

        state.resetTurnDrawState();
        advanceTurn(state, 1);
        return true;
    }

    public boolean acceptWildDrawFour(GameState state, String playerId) {
        PlayerState challenger = requireCurrentPlayer(state, playerId);
        if (!state.isWildDrawFourChallengePending()) {
            throw new InvalidMoveException("There is no Wild Draw Four to accept right now.");
        }

        drawCards(state, challenger, 4);
        state.setPendingDrawCount(0);
        state.clearWildDrawFourChallenge();

        PlayerState pendingWinner = findPlayerById(state, state.getPendingWinnerPlayerId());
        if (pendingWinner != null && pendingWinner.handSize() == 0) {
            state.markWinner(pendingWinner);
            return false;
        }

        state.clearPendingWinner();
        advanceTurn(state, 1);
        return true;
    }

    public boolean challengeWildDrawFour(GameState state, String playerId) {
        PlayerState challenger = requireCurrentPlayer(state, playerId);
        if (!state.isWildDrawFourChallengePending()) {
            throw new InvalidMoveException("There is no Wild Draw Four to challenge right now.");
        }

        PlayerState offender = findPlayerById(state, state.getWildDrawFourPlayerId());
        if (offender == null) {
            throw new InvalidMoveException("The Wild Draw Four source player could not be found.");
        }

        boolean wasLegal = state.isWildDrawFourPlayLegal();
        state.setPendingDrawCount(0);
        state.clearWildDrawFourChallenge();

        if (wasLegal) {
            drawCards(state, challenger, 6);
            PlayerState pendingWinner = findPlayerById(state, state.getPendingWinnerPlayerId());
            if (pendingWinner != null && pendingWinner.handSize() == 0) {
                state.markWinner(pendingWinner);
                return false;
            }

            state.clearPendingWinner();
            advanceTurn(state, 1);
            return true;
        }

        drawCards(state, offender, 4);
        state.clearPendingWinner();
        return false;
    }

    public void advanceTurn(GameState state, int steps) {
        state.resetTurnDrawState();
        state.setCurrentPlayerIndex(stepFrom(state, steps));
    }

    private PlayerState requireCurrentPlayer(GameState state, String playerId) {
        if (!state.isStarted()) {
            throw new InvalidMoveException("The game has not started yet.");
        }
        if (state.isFinished()) {
            throw new InvalidMoveException("The game is already over.");
        }

        PlayerState currentPlayer = state.getCurrentPlayer();
        if (!currentPlayer.getPlayerId().equals(playerId)) {
            throw new InvalidMoveException("It is not your turn.");
        }
        return currentPlayer;
    }

    private void validateHandIndex(PlayerState player, int handIndex) {
        if (handIndex < 0 || handIndex >= player.handSize()) {
            throw new InvalidMoveException("That card is no longer in your hand.");
        }
    }

    private List<Integer> getStandardPlayableCardIndexes(GameState state, PlayerState player) {
        List<Integer> playable = new ArrayList<>();
        for (int index = 0; index < player.handSize(); index++) {
            if (canPlay(player.getCardAt(index), state)) {
                playable.add(index);
            }
        }
        return playable;
    }

    private List<Integer> getCustomActionCardIndexes(PlayerState player) {
        List<Integer> customIndexes = new ArrayList<>();
        for (int index = 0; index < player.handSize(); index++) {
            CardType type = player.getCardAt(index).type();
            if (type == CardType.GROUP_DRAW || type == CardType.TRIPLE_PEEK) {
                customIndexes.add(index);
            }
        }
        return customIndexes;
    }

    private boolean playGroupDraw(GameState state, PlayerState player, int handIndex) {
        Card playedCard = player.removeCardAt(handIndex);
        state.getDiscardPile().push(playedCard);
        state.setCurrentColor(playedCard.color());
        state.resetTurnDrawState();

        for (PlayerState otherPlayer : state.getPlayers()) {
            drawCards(state, otherPlayer, 1);
        }

        int drawnCardIndex = player.handSize() - 1;
        Card drawnCard = player.getCardAt(drawnCardIndex);
        if (canPlay(drawnCard, state)) {
            return autoPlayAcquiredCard(state, player, drawnCardIndex);
        }

        state.resetTurnDrawState();
        advanceTurn(state, 1);
        return true;
    }

    private boolean playTriplePeek(GameState state, PlayerState player, int handIndex) {
        List<Card> options = takeTriplePeekOptions(state);
        Card playedCard = player.removeCardAt(handIndex);
        state.getDiscardPile().push(playedCard);
        state.setCurrentColor(playedCard.color());
        state.resetTurnDrawState();
        state.setTriplePeekChoicePending(true);
        state.setTriplePeekPlayerId(player.getPlayerId());
        state.setTriplePeekPlayerUsername(player.getUsername());
        state.setTriplePeekOptions(options);
        return false;
    }

    private void resolveActionEffect(GameState state, PlayerState player, Card playedCard, CardColor chosenColor, boolean wildDrawFourLegal) {
        switch (playedCard.type()) {
            case NUMBER -> {
                state.setCurrentColor(playedCard.color());
                advanceTurn(state, 1);
            }
            case SKIP -> {
                state.setCurrentColor(playedCard.color());
                advanceTurn(state, 2);
            }
            case REVERSE -> {
                state.setCurrentColor(playedCard.color());
                if (state.getPlayers().size() == 2) {
                    advanceTurn(state, 0);
                } else {
                    state.setDirection(state.getDirection() * -1);
                    advanceTurn(state, 1);
                }
            }
            case DRAW_TWO -> {
                state.setCurrentColor(playedCard.color());
                state.setPendingDrawCount(state.getPendingDrawCount() + 2);
                advanceTurn(state, 1);
            }
            case WILD -> {
                state.setCurrentColor(requireChosenColor(chosenColor));
                advanceTurn(state, 1);
            }
            case WILD_DRAW_FOUR -> {
                state.setCurrentColor(requireChosenColor(chosenColor));
                state.setPendingDrawCount(4);
                state.setWildDrawFourChallengePending(true);
                state.setWildDrawFourPlayerId(player.getPlayerId());
                state.setWildDrawFourPlayerUsername(player.getUsername());
                state.setWildDrawFourPlayLegal(wildDrawFourLegal);
                advanceTurn(state, 1);
            }
            default -> throw new InvalidMoveException("This card type is not supported in Phase 5.");
        }
    }

    private boolean autoPlayAcquiredCard(GameState state, PlayerState player, int handIndex) {
        Card drawnCard = player.getCardAt(handIndex);
        boolean wildDrawFourLegal = drawnCard.type() != CardType.WILD_DRAW_FOUR
                || isWildDrawFourLegal(state, player, handIndex);
        CardColor chosenColor = requiresChosenColor(drawnCard)
                ? chooseAutomaticColor(player, handIndex, state.getCurrentColor())
                : null;

        Card playedCard = player.removeCardAt(handIndex);
        state.getDiscardPile().push(playedCard);
        state.resetTurnDrawState();
        resolveActionEffect(state, player, playedCard, chosenColor, wildDrawFourLegal);

        if (player.handSize() == 0 && !state.isWildDrawFourChallengePending()) {
            state.markWinner(player);
            return false;
        }
        if (player.handSize() == 0 && state.isWildDrawFourChallengePending()) {
            state.setPendingWinner(player.getPlayerId(), player.getUsername());
        }

        return true;
    }

    private boolean requiresChosenColor(Card card) {
        return card.type() == CardType.WILD || card.type() == CardType.WILD_DRAW_FOUR;
    }

    private CardColor chooseAutomaticColor(PlayerState player, int excludedHandIndex, CardColor fallbackColor) {
        CardColor bestColor = fallbackColor != null && fallbackColor != CardColor.BLACK ? fallbackColor : CardColor.RED;
        int bestCount = -1;
        for (CardColor color : CardColor.values()) {
            if (color == CardColor.BLACK) {
                continue;
            }
            int count = 0;
            for (int index = 0; index < player.handSize(); index++) {
                if (index == excludedHandIndex) {
                    continue;
                }
                if (player.getCardAt(index).color() == color) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestCount = count;
                bestColor = color;
            }
        }
        return bestColor;
    }

    private CardColor requireChosenColor(CardColor chosenColor) {
        if (chosenColor == null || chosenColor == CardColor.BLACK) {
            throw new InvalidMoveException("Choose a valid color before playing a wild card.");
        }
        return chosenColor;
    }

    private int stepFrom(GameState state, int steps) {
        int playerCount = state.getPlayers().size();
        int index = state.getCurrentPlayerIndex();
        for (int step = 0; step < steps; step++) {
            index = Math.floorMod(index + state.getDirection(), playerCount);
        }
        return index;
    }

    private boolean isWildDrawFourLegal(GameState state, PlayerState player, int wildDrawFourIndex) {
        for (int index = 0; index < player.handSize(); index++) {
            if (index == wildDrawFourIndex) {
                continue;
            }

            Card otherCard = player.getCardAt(index);
            if (otherCard.type() == CardType.WILD_DRAW_FOUR) {
                continue;
            }
            if (canPlayByActiveColorOrTop(otherCard, state)) {
                return false;
            }
        }
        return true;
    }

    private PlayerState findPlayerById(GameState state, String playerId) {
        if (playerId == null) {
            return null;
        }
        return state.getPlayers().stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    private List<Card> takeTriplePeekOptions(GameState state) {
        refillDrawPileIfNeeded(state, 3);
        if (state.getDrawPile().isEmpty()) {
            throw new InvalidMoveException("No cards are available for Triple Peek.");
        }

        int cardCount = Math.min(3, state.getDrawPile().size());
        List<Card> options = new ArrayList<>(cardCount);
        for (int index = 0; index < cardCount; index++) {
            options.add(state.getDrawPile().removeFirst());
        }
        return options;
    }

    private void drawCards(GameState state, PlayerState player, int count) {
        for (int draw = 0; draw < count; draw++) {
            refillDrawPileIfNeeded(state, 1);
            if (state.getDrawPile().isEmpty()) {
                throw new InvalidMoveException("No cards are available to draw.");
            }
            player.addCard(state.getDrawPile().removeFirst());
        }
    }

    private void refillDrawPileIfNeeded(GameState state, int minimumCardsNeeded) {
        if (state.getDrawPile().size() >= minimumCardsNeeded) {
            return;
        }
        if (state.getDiscardPile().size() <= 1) {
            return;
        }

        Card topDiscard = state.getDiscardPile().removeFirst();
        List<Card> reshuffledCards = new ArrayList<>(state.getDiscardPile());
        Collections.shuffle(reshuffledCards);

        state.getDiscardPile().clear();
        state.getDiscardPile().addFirst(topDiscard);
        state.getDrawPile().addAll(reshuffledCards);
    }
}
