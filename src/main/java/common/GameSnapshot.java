package common;

import game.model.Card;
import game.model.CardColor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record GameSnapshot(
        String roomId,
        Card topDiscard,
        CardColor currentColor,
        String currentTurnPlayerId,
        String currentTurnUsername,
        int pendingDrawCount,
        boolean wildDrawFourChallengePending,
        String wildDrawFourPlayerId,
        String wildDrawFourPlayerUsername,
        boolean triplePeekChoicePending,
        String triplePeekPlayerId,
        String triplePeekPlayerUsername,
        boolean canAcceptWildDrawFour,
        boolean canChallengeWildDrawFour,
        boolean started,
        boolean yourTurn,
        boolean canDraw,
        boolean canEndTurn,
        boolean finished,
        String winnerPlayerId,
        String winnerUsername,
        List<Card> yourHand,
        List<Integer> playableCardIndexes,
        List<PlayerInfo> players
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
