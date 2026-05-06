package game.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class GameState {
    private final List<PlayerState> players;
    private Deque<Card> drawPile = new ArrayDeque<>();
    private Deque<Card> discardPile = new ArrayDeque<>();
    private int currentPlayerIndex;
    private int direction = 1;
    private int pendingDrawCount;
    private CardColor currentColor;
    private boolean wildDrawFourChallengePending;
    private String wildDrawFourPlayerId;
    private String wildDrawFourPlayerUsername;
    private boolean wildDrawFourPlayLegal;
    private boolean triplePeekChoicePending;
    private String triplePeekPlayerId;
    private String triplePeekPlayerUsername;
    private List<Card> triplePeekOptions = List.of();
    private String pendingWinnerPlayerId;
    private String pendingWinnerUsername;
    private boolean started;
    private boolean finished;
    private String winnerPlayerId;
    private String winnerUsername;
    private boolean currentPlayerHasDrawn;
    private int drawnCardIndexThisTurn = -1;

    public GameState(List<PlayerState> players) {
        this.players = players;
    }

    public List<PlayerState> getPlayers() {
        return players;
    }

    public Deque<Card> getDrawPile() {
        return drawPile;
    }

    public void setDrawPile(Deque<Card> drawPile) {
        this.drawPile = drawPile;
    }

    public Deque<Card> getDiscardPile() {
        return discardPile;
    }

    public void setDiscardPile(Deque<Card> discardPile) {
        this.discardPile = discardPile;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getPendingDrawCount() {
        return pendingDrawCount;
    }

    public void setPendingDrawCount(int pendingDrawCount) {
        this.pendingDrawCount = pendingDrawCount;
    }

    public CardColor getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(CardColor currentColor) {
        this.currentColor = currentColor;
    }

    public boolean isWildDrawFourChallengePending() {
        return wildDrawFourChallengePending;
    }

    public void setWildDrawFourChallengePending(boolean wildDrawFourChallengePending) {
        this.wildDrawFourChallengePending = wildDrawFourChallengePending;
    }

    public String getWildDrawFourPlayerId() {
        return wildDrawFourPlayerId;
    }

    public void setWildDrawFourPlayerId(String wildDrawFourPlayerId) {
        this.wildDrawFourPlayerId = wildDrawFourPlayerId;
    }

    public String getWildDrawFourPlayerUsername() {
        return wildDrawFourPlayerUsername;
    }

    public void setWildDrawFourPlayerUsername(String wildDrawFourPlayerUsername) {
        this.wildDrawFourPlayerUsername = wildDrawFourPlayerUsername;
    }

    public boolean isWildDrawFourPlayLegal() {
        return wildDrawFourPlayLegal;
    }

    public void setWildDrawFourPlayLegal(boolean wildDrawFourPlayLegal) {
        this.wildDrawFourPlayLegal = wildDrawFourPlayLegal;
    }

    public boolean isTriplePeekChoicePending() {
        return triplePeekChoicePending;
    }

    public void setTriplePeekChoicePending(boolean triplePeekChoicePending) {
        this.triplePeekChoicePending = triplePeekChoicePending;
    }

    public String getTriplePeekPlayerId() {
        return triplePeekPlayerId;
    }

    public void setTriplePeekPlayerId(String triplePeekPlayerId) {
        this.triplePeekPlayerId = triplePeekPlayerId;
    }

    public String getTriplePeekPlayerUsername() {
        return triplePeekPlayerUsername;
    }

    public void setTriplePeekPlayerUsername(String triplePeekPlayerUsername) {
        this.triplePeekPlayerUsername = triplePeekPlayerUsername;
    }

    public List<Card> getTriplePeekOptions() {
        return triplePeekOptions;
    }

    public void setTriplePeekOptions(List<Card> triplePeekOptions) {
        this.triplePeekOptions = List.copyOf(triplePeekOptions);
    }

    public String getPendingWinnerPlayerId() {
        return pendingWinnerPlayerId;
    }

    public String getPendingWinnerUsername() {
        return pendingWinnerUsername;
    }

    public void setPendingWinner(String pendingWinnerPlayerId, String pendingWinnerUsername) {
        this.pendingWinnerPlayerId = pendingWinnerPlayerId;
        this.pendingWinnerUsername = pendingWinnerUsername;
    }

    public void clearPendingWinner() {
        this.pendingWinnerPlayerId = null;
        this.pendingWinnerUsername = null;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getWinnerPlayerId() {
        return winnerPlayerId;
    }

    public void setWinnerPlayerId(String winnerPlayerId) {
        this.winnerPlayerId = winnerPlayerId;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    public void setWinnerUsername(String winnerUsername) {
        this.winnerUsername = winnerUsername;
    }

    public boolean hasCurrentPlayerDrawn() {
        return currentPlayerHasDrawn;
    }

    public void setCurrentPlayerHasDrawn(boolean currentPlayerHasDrawn) {
        this.currentPlayerHasDrawn = currentPlayerHasDrawn;
    }

    public int getDrawnCardIndexThisTurn() {
        return drawnCardIndexThisTurn;
    }

    public void setDrawnCardIndexThisTurn(int drawnCardIndexThisTurn) {
        this.drawnCardIndexThisTurn = drawnCardIndexThisTurn;
    }

    public PlayerState getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public Card getTopDiscard() {
        return discardPile.peekFirst();
    }

    public void markWinner(PlayerState winner) {
        finished = true;
        winnerPlayerId = winner.getPlayerId();
        winnerUsername = winner.getUsername();
        clearPendingWinner();
        clearTriplePeekChoice();
    }

    public void clearWildDrawFourChallenge() {
        wildDrawFourChallengePending = false;
        wildDrawFourPlayerId = null;
        wildDrawFourPlayerUsername = null;
        wildDrawFourPlayLegal = false;
    }

    public void clearTriplePeekChoice() {
        triplePeekChoicePending = false;
        triplePeekPlayerId = null;
        triplePeekPlayerUsername = null;
        triplePeekOptions = List.of();
    }

    public void resetTurnDrawState() {
        currentPlayerHasDrawn = false;
        drawnCardIndexThisTurn = -1;
    }
}
