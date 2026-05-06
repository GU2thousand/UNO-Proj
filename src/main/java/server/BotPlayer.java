package server;

import game.model.PlayerState;

public record BotPlayer(String playerId, String username) {
    public BotPlayer(String roomId, int botNumber) {
        this("bot-" + roomId + "-" + botNumber, "Bot " + botNumber);
    }

    public PlayerState toPlayerState() {
        return new PlayerState(playerId, username, true);
    }
}
