package server;

import common.GameSnapshot;
import common.Message;
import common.MessageType;
import common.MatchSummary;
import common.PlayerInfo;
import common.PlayerStats;
import common.RoomSnapshot;
import common.payload.GameOverPayload;
import common.payload.PrivateUpdatePayload;
import common.payload.PlayerStatsPayload;
import common.payload.TriplePeekOptionsPayload;
import common.payload.TurnUpdatePayload;
import game.logic.RuleEngine;
import game.model.Card;
import game.model.CardColor;
import game.model.CardType;
import game.model.Deck;
import game.model.GameState;
import game.model.PlayerState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import persistence.MatchRepository;
import persistence.PlayerRepository;

public class GameRoom {
    private final String roomId;
    private int maxPlayers;
    private final Map<String, ClientHandler> handlers = new LinkedHashMap<>();
    private final Map<String, BotPlayer> bots = new LinkedHashMap<>();
    private final RuleEngine ruleEngine = new RuleEngine();
    private final SimpleBotStrategy botStrategy = new SimpleBotStrategy();
    private final ScheduledExecutorService botExecutor = Executors.newSingleThreadScheduledExecutor();
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private String hostPlayerId;
    private GameState gameState;
    private long botTurnToken;
    private int nextBotNumber = 1;
    private boolean matchRecorded;

    public GameRoom(String roomId, int maxPlayers, String hostPlayerId, PlayerRepository playerRepository, MatchRepository matchRepository) {
        this.roomId = roomId;
        this.maxPlayers = maxPlayers;
        this.hostPlayerId = hostPlayerId;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
    }

    public synchronized void addPlayer(ClientHandler handler) {
        if (gameState != null && gameState.isStarted()) {
            throw new IllegalStateException("The game in this room has already started.");
        }
        if (handlers.containsKey(handler.getPlayerId())) {
            return;
        }
        if (totalPlayerCount() >= maxPlayers) {
            throw new IllegalStateException("Room " + roomId + " is already full.");
        }

        handlers.put(handler.getPlayerId(), handler);
        handler.setCurrentRoomId(roomId);
    }

    public synchronized void addBot(String requestingPlayerId) {
        if (!hostPlayerId.equals(requestingPlayerId)) {
            throw new IllegalStateException("Only the room host can add a bot.");
        }
        if (gameState != null && gameState.isStarted()) {
            throw new IllegalStateException("Bots can only be added before the game starts.");
        }
        if (totalPlayerCount() >= maxPlayers) {
            throw new IllegalStateException("The room is already full.");
        }

        BotPlayer botPlayer = new BotPlayer(roomId, nextBotNumber++);
        bots.put(botPlayer.playerId(), botPlayer);
    }

    public synchronized void updateMaxPlayers(String requestingPlayerId, int newMaxPlayers) {
        if (!hostPlayerId.equals(requestingPlayerId)) {
            throw new IllegalStateException("Only the room host can change the room size.");
        }
        if (gameState != null && gameState.isStarted() && !gameState.isFinished()) {
            throw new IllegalStateException("Room size can only be changed before the game starts.");
        }
        if (newMaxPlayers < 2 || newMaxPlayers > 4) {
            throw new IllegalArgumentException("Room size must be between 2 and 4.");
        }
        if (newMaxPlayers < totalPlayerCount()) {
            throw new IllegalStateException("Room size cannot be smaller than the number of players already in the room.");
        }
        maxPlayers = newMaxPlayers;
    }

    public synchronized void removePlayer(String playerId) {
        handlers.remove(playerId);

        if (gameState != null) {
            gameState.getPlayers().removeIf(player -> player.getPlayerId().equals(playerId));
            if (gameState.getCurrentPlayerIndex() >= gameState.getPlayers().size() && !gameState.getPlayers().isEmpty()) {
                gameState.setCurrentPlayerIndex(0);
            }
        }

        if (playerId.equals(hostPlayerId) && !handlers.isEmpty()) {
            hostPlayerId = handlers.values().iterator().next().getPlayerId();
        }
    }

    public synchronized void startGame(String requestingPlayerId) {
        if (!hostPlayerId.equals(requestingPlayerId)) {
            throw new IllegalStateException("Only the room host can start the game.");
        }
        if (totalPlayerCount() < 2) {
            throw new IllegalStateException("At least two players are required.");
        }
        if (totalPlayerCount() != maxPlayers) {
            throw new IllegalStateException("The room is not full yet.");
        }
        if (gameState != null && gameState.isStarted() && !gameState.isFinished()) {
            throw new IllegalStateException("Game already started.");
        }

        List<PlayerState> players = new ArrayList<>();
        for (ClientHandler handler : handlers.values()) {
            players.add(new PlayerState(handler.getPlayerId(), handler.getUsername()));
        }
        for (BotPlayer bot : bots.values()) {
            players.add(bot.toPlayerState());
        }

        Deck deck = new Deck();
        for (int round = 0; round < 7; round++) {
            for (PlayerState player : players) {
                player.addCard(deck.draw());
            }
        }

        ArrayDeque<Card> drawPile = new ArrayDeque<>(deck.remainingCards());
        ArrayDeque<Card> discardPile = new ArrayDeque<>();
        Card firstDiscard = drawStartingDiscard(drawPile);
        discardPile.push(firstDiscard);

        gameState = new GameState(players);
        gameState.setDrawPile(drawPile);
        gameState.setDiscardPile(discardPile);
        gameState.setCurrentPlayerIndex(0);
        gameState.setDirection(1);
        gameState.setPendingDrawCount(0);
        gameState.setCurrentColor(firstDiscard.color());
        gameState.setStarted(true);
        gameState.resetTurnDrawState();
        matchRecorded = false;
    }

    public synchronized void handlePlayCard(String playerId, int handIndex, game.model.CardColor chosenColor) {
        ensureStarted();
        PlayerState player = findPlayerState(playerId);
        if (player == null) {
            throw new IllegalStateException("Player is no longer in this game.");
        }
        if (handIndex < 0 || handIndex >= player.handSize()) {
            throw new IllegalStateException("That card is no longer in your hand.");
        }
        Card selectedCard = player.getCardAt(handIndex);
        boolean turnAdvanced = ruleEngine.playCard(gameState, playerId, handIndex, chosenColor);
        broadcastAfterStateChange(turnAdvanced);
        if (selectedCard.type() == CardType.GROUP_DRAW) {
            sendPrivateUpdate(playerId, "Group Draw resolved. Everyone drew 1 card, and your draw-phase result has been applied.");
        } else if (selectedCard.type() == CardType.TRIPLE_PEEK && !player.isBot()) {
            sendTriplePeekOptions(playerId);
        }
    }

    public synchronized void handleDrawCard(String playerId) {
        ensureStarted();
        boolean turnAdvanced = ruleEngine.drawCard(gameState, playerId);
        broadcastAfterStateChange(turnAdvanced);
    }

    public synchronized void handleEndTurn(String playerId) {
        ensureStarted();
        boolean turnAdvanced = ruleEngine.endTurn(gameState, playerId);
        broadcastAfterStateChange(turnAdvanced);
    }

    public synchronized void handleAcceptWildDrawFour(String playerId) {
        ensureStarted();
        boolean turnAdvanced = ruleEngine.acceptWildDrawFour(gameState, playerId);
        broadcastAfterStateChange(turnAdvanced);
    }

    public synchronized void handleChallengeWildDrawFour(String playerId) {
        ensureStarted();
        boolean turnAdvanced = ruleEngine.challengeWildDrawFour(gameState, playerId);
        broadcastAfterStateChange(turnAdvanced);
    }

    public synchronized void handleTriplePeekChoice(String playerId, int optionIndex) {
        ensureStarted();
        boolean turnAdvanced = ruleEngine.chooseTriplePeekOption(gameState, playerId, optionIndex);
        broadcastAfterStateChange(turnAdvanced);
        sendPrivateUpdate(playerId, "Triple Peek resolved. Your selected card has been added to your hand.");
    }

    public synchronized RoomSnapshot toRoomSnapshot() {
        List<PlayerInfo> players = new ArrayList<>();
        boolean started = isStarted();
        for (ClientHandler handler : handlers.values()) {
            int cardCount = 0;
            if (started) {
                PlayerState playerState = findPlayerState(handler.getPlayerId());
                cardCount = playerState == null ? 0 : playerState.handSize();
            }
            players.add(new PlayerInfo(handler.getPlayerId(), handler.getUsername(), cardCount, false));
        }
        for (BotPlayer bot : bots.values()) {
            int cardCount = 0;
            if (started) {
                PlayerState playerState = findPlayerState(bot.playerId());
                cardCount = playerState == null ? 0 : playerState.handSize();
            }
            players.add(new PlayerInfo(bot.playerId(), bot.username(), cardCount, true));
        }
        return new RoomSnapshot(roomId, hostPlayerId, maxPlayers, started, List.copyOf(players));
    }

    public synchronized GameSnapshot toGameSnapshotFor(String playerId) {
        if (!isStarted()) {
            return null;
        }

        PlayerState viewer = findPlayerState(playerId);
        if (viewer == null) {
            return null;
        }

        List<PlayerInfo> players = new ArrayList<>();
        for (PlayerState player : gameState.getPlayers()) {
            players.add(new PlayerInfo(player.getPlayerId(), player.getUsername(), player.handSize(), player.isBot()));
        }

        PlayerState currentPlayer = gameState.getCurrentPlayer();
        boolean yourTurn = currentPlayer.getPlayerId().equals(playerId) && !gameState.isFinished();
        List<Integer> playableCardIndexes = yourTurn
                ? ruleEngine.getPlayableCardIndexes(gameState, viewer)
                : List.of();
        boolean canAcceptWildDrawFour = yourTurn && gameState.isWildDrawFourChallengePending();
        boolean canChallengeWildDrawFour = canAcceptWildDrawFour;
        boolean canDraw = yourTurn && ruleEngine.canDraw(gameState, viewer);
        boolean canEndTurn = yourTurn
                && !gameState.isTriplePeekChoicePending()
                && gameState.hasCurrentPlayerDrawn()
                && !playableCardIndexes.isEmpty();
        return new GameSnapshot(
                roomId,
                gameState.getTopDiscard(),
                gameState.getCurrentColor(),
                currentPlayer.getPlayerId(),
                currentPlayer.getUsername(),
                gameState.getPendingDrawCount(),
                gameState.isWildDrawFourChallengePending(),
                gameState.getWildDrawFourPlayerId(),
                gameState.getWildDrawFourPlayerUsername(),
                gameState.isTriplePeekChoicePending(),
                gameState.getTriplePeekPlayerId(),
                gameState.getTriplePeekPlayerUsername(),
                canAcceptWildDrawFour,
                canChallengeWildDrawFour,
                true,
                yourTurn,
                canDraw,
                canEndTurn,
                gameState.isFinished(),
                gameState.getWinnerPlayerId(),
                gameState.getWinnerUsername(),
                List.copyOf(viewer.getHand()),
                List.copyOf(playableCardIndexes),
                List.copyOf(players)
        );
    }

    public synchronized void broadcastRoomUpdate() {
        RoomSnapshot snapshot = toRoomSnapshot();
        Message message = new Message(MessageType.ROOM_UPDATE, null, snapshot);
        for (ClientHandler handler : handlers.values()) {
            handler.send(message);
        }
    }

    public synchronized void broadcastGameState() {
        if (!isStarted()) {
            return;
        }
        for (ClientHandler handler : handlers.values()) {
            GameSnapshot snapshot = toGameSnapshotFor(handler.getPlayerId());
            if (snapshot != null) {
                handler.send(new Message(MessageType.GAME_STATE, handler.getPlayerId(), snapshot));
            }
        }
    }

    public synchronized void broadcastTurnUpdate() {
        if (!isStarted() || gameState.isFinished()) {
            return;
        }
        PlayerState currentPlayer = gameState.getCurrentPlayer();
        Message message = new Message(
                MessageType.TURN_UPDATE,
                currentPlayer.getPlayerId(),
                new TurnUpdatePayload(currentPlayer.getPlayerId(), currentPlayer.getUsername())
        );
        for (ClientHandler handler : handlers.values()) {
            handler.send(message);
        }
    }

    public synchronized void broadcastGameOver() {
        if (!isStarted() || !gameState.isFinished()) {
            return;
        }
        Message message = new Message(
                MessageType.GAME_OVER,
                gameState.getWinnerPlayerId(),
                new GameOverPayload(gameState.getWinnerPlayerId(), gameState.getWinnerUsername())
        );
        for (ClientHandler handler : handlers.values()) {
            handler.send(message);
        }
    }

    public synchronized boolean isEmpty() {
        return handlers.isEmpty();
    }

    public synchronized boolean isStarted() {
        return gameState != null && gameState.isStarted();
    }

    public String getRoomId() {
        return roomId;
    }

    public synchronized void shutdown() {
        botTurnToken++;
        botExecutor.shutdownNow();
    }

    public synchronized void scheduleBotTurnIfNeeded() {
        botTurnToken++;
        long scheduledToken = botTurnToken;
        if (!isStarted() || gameState.isFinished()) {
            return;
        }

        PlayerState currentPlayer = gameState.getCurrentPlayer();
        if (!currentPlayer.isBot()) {
            return;
        }

        long delayMillis = ThreadLocalRandom.current().nextLong(300, 801);
        botExecutor.schedule(() -> executeBotTurn(scheduledToken), delayMillis, TimeUnit.MILLISECONDS);
    }

    private void ensureStarted() {
        if (!isStarted()) {
            throw new IllegalStateException("The game has not started yet.");
        }
    }

    private void broadcastAfterStateChange(boolean turnAdvanced) {
        broadcastRoomUpdate();
        broadcastGameState();
        if (gameState.isFinished()) {
            persistMatchIfNeeded();
            broadcastGameOver();
            return;
        }
        if (turnAdvanced) {
            broadcastTurnUpdate();
        }
        scheduleBotTurnIfNeeded();
    }

    private Card drawStartingDiscard(ArrayDeque<Card> drawPile) {
        int attempts = drawPile.size();
        for (int attempt = 0; attempt < attempts; attempt++) {
            Card candidate = drawPile.removeFirst();
            if (candidate.type() == game.model.CardType.NUMBER) {
                return candidate;
            }
            drawPile.addLast(candidate);
        }
        throw new IllegalStateException("Could not find a valid opening discard card.");
    }

    private PlayerState findPlayerState(String playerId) {
        if (gameState == null) {
            return null;
        }
        return gameState.getPlayers().stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    private int totalPlayerCount() {
        return handlers.size() + bots.size();
    }

    private void executeBotTurn(long expectedToken) {
        synchronized (this) {
            if (expectedToken != botTurnToken || !isStarted() || gameState.isFinished()) {
                return;
            }

            PlayerState bot = gameState.getCurrentPlayer();
            if (!bot.isBot()) {
                return;
            }

            if (gameState.isWildDrawFourChallengePending()) {
                boolean turnAdvanced = botStrategy.shouldChallengeWildDrawFour()
                        ? ruleEngine.challengeWildDrawFour(gameState, bot.getPlayerId())
                        : ruleEngine.acceptWildDrawFour(gameState, bot.getPlayerId());
                broadcastAfterStateChange(turnAdvanced);
                return;
            }

            if (gameState.isTriplePeekChoicePending()) {
                int optionIndex = botStrategy.chooseTriplePeekOption(gameState.getTriplePeekOptions(), ruleEngine, gameState);
                boolean turnAdvanced = ruleEngine.chooseTriplePeekOption(gameState, bot.getPlayerId(), optionIndex);
                broadcastAfterStateChange(turnAdvanced);
                return;
            }

            List<Integer> playableIndexes = ruleEngine.getPlayableCardIndexes(gameState, bot);
            if (!playableIndexes.isEmpty()) {
                int handIndex = botStrategy.chooseFirstPlayableCard(playableIndexes);
                Card selectedCard = bot.getCardAt(handIndex);
                CardColor chosenColor = (selectedCard.type() == CardType.WILD || selectedCard.type() == CardType.WILD_DRAW_FOUR)
                        ? botStrategy.chooseColor(bot.getHand(), handIndex)
                        : null;
                boolean turnAdvanced = ruleEngine.playCard(
                        gameState,
                        bot.getPlayerId(),
                        handIndex,
                        chosenColor
                );
                broadcastAfterStateChange(turnAdvanced);
                return;
            }

            if (ruleEngine.canDraw(gameState, bot)) {
                boolean turnAdvanced = ruleEngine.drawCard(gameState, bot.getPlayerId());
                broadcastAfterStateChange(turnAdvanced);
                return;
            }

            if (gameState.hasCurrentPlayerDrawn()) {
                boolean turnAdvanced = ruleEngine.endTurn(gameState, bot.getPlayerId());
                broadcastAfterStateChange(turnAdvanced);
            }
        }
    }

    private void sendTriplePeekOptions(String playerId) {
        ClientHandler handler = handlers.get(playerId);
        if (handler == null) {
            return;
        }
        handler.send(new Message(
                MessageType.TRIPLE_PEEK_OPTIONS,
                playerId,
                new TriplePeekOptionsPayload(gameState.getTriplePeekOptions())
        ));
    }

    private void sendPrivateUpdate(String playerId, String message) {
        ClientHandler handler = handlers.get(playerId);
        if (handler == null) {
            return;
        }
        handler.send(new Message(MessageType.PRIVATE_UPDATE, playerId, new PrivateUpdatePayload(message)));
    }

    private void persistMatchIfNeeded() {
        if (matchRecorded) {
            return;
        }

        matchRepository.recordMatchResult(List.copyOf(gameState.getPlayers()), gameState.getWinnerUsername());
        matchRecorded = true;
        broadcastPlayerStatsUpdates();
    }

    private void broadcastPlayerStatsUpdates() {
        for (ClientHandler handler : handlers.values()) {
            PlayerStats stats = playerRepository.getPlayerStats(handler.getUsername());
            List<MatchSummary> recentMatches = matchRepository.getRecentMatches(handler.getUsername(), 5);
            handler.send(new Message(
                    MessageType.PLAYER_STATS_UPDATE,
                    handler.getPlayerId(),
                    new PlayerStatsPayload(stats, recentMatches)
            ));
        }
    }
}
