package game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerState {
    private final String playerId;
    private final String username;
    private final boolean bot;
    private final List<Card> hand = new ArrayList<>();

    public PlayerState(String playerId, String username) {
        this(playerId, username, false);
    }

    public PlayerState(String playerId, String username, boolean bot) {
        this.playerId = playerId;
        this.username = username;
        this.bot = bot;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public boolean isBot() {
        return bot;
    }

    public List<Card> getHand() {
        return Collections.unmodifiableList(hand);
    }

    public Card getCardAt(int index) {
        return hand.get(index);
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public Card removeCardAt(int index) {
        return hand.remove(index);
    }

    public void clearHand() {
        hand.clear();
    }

    public int handSize() {
        return hand.size();
    }
}
