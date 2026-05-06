package server;

import common.Message;
import common.MessageType;
import common.MatchSummary;
import common.PlayerStats;
import common.payload.ConnectRequest;
import common.payload.ConnectedPayload;
import common.payload.CreateRoomRequest;
import common.payload.ErrorPayload;
import common.payload.JoinRoomRequest;
import common.payload.PlayCardRequest;
import common.payload.TriplePeekChoiceRequest;
import common.payload.UpdateMaxPlayersRequest;
import game.logic.InvalidMoveException;
import persistence.MatchRepository;
import persistence.PlayerRepository;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final RoomManager roomManager;

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private volatile boolean running = true;
    private String playerId;
    private String username;
    private String currentRoomId;

    public ClientHandler(Socket socket, RoomManager roomManager) {
        this.socket = socket;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        try (socket) {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            while (running) {
                Object incoming = input.readObject();
                if (incoming instanceof Message message) {
                    handleMessage(message);
                }
            }
        } catch (EOFException ignored) {
            // The client closed the socket.
        } catch (Exception exception) {
            if (running) {
                exception.printStackTrace();
            }
        } finally {
            cleanup();
        }
    }

    public synchronized void send(Message message) {
        if (output == null || socket.isClosed()) {
            return;
        }
        try {
            output.reset();
            output.writeObject(message);
            output.flush();
        } catch (IOException exception) {
            running = false;
        }
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public void setCurrentRoomId(String currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    private void handleMessage(Message message) {
        try {
            switch (message.type()) {
                case CONNECT -> handleConnect(message.payload());
                case CREATE_ROOM -> handleCreateRoom(message.payload());
                case JOIN_ROOM -> handleJoinRoom(message.payload());
                case ADD_BOT -> handleAddBot();
                case UPDATE_MAX_PLAYERS -> handleUpdateMaxPlayers(message.payload());
                case START_GAME -> handleStartGame();
                case PLAY_CARD -> handlePlayCard(message.payload());
                case DRAW_CARD -> handleDrawCard();
                case END_TURN -> handleEndTurn();
                case TRIPLE_PEEK_CHOICE -> handleTriplePeekChoice(message.payload());
                case ACCEPT_WILD_DRAW_FOUR -> handleAcceptWildDrawFour();
                case CHALLENGE_WILD_DRAW_FOUR -> handleChallengeWildDrawFour();
                default -> sendError("Unsupported message type: " + message.type());
            }
        } catch (InvalidMoveException exception) {
            sendInvalidMove(exception.getMessage());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            sendError(exception.getMessage());
        }
    }

    private void handleConnect(Object payload) {
        if (playerId != null) {
            throw new IllegalStateException("You are already connected.");
        }
        if (!(payload instanceof ConnectRequest request)) {
            throw new IllegalArgumentException("Invalid connect payload.");
        }

        String requestedUsername = request.username() == null ? "" : request.username().trim();
        if (requestedUsername.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }

        playerId = UUID.randomUUID().toString();
        username = requestedUsername;
        PlayerRepository playerRepository = roomManager.getPlayerRepository();
        MatchRepository matchRepository = roomManager.getMatchRepository();
        playerRepository.createPlayerIfNotExists(username);
        PlayerStats stats = playerRepository.getPlayerStats(username);
        java.util.List<MatchSummary> recentMatches = matchRepository.getRecentMatches(username, 5);
        send(new Message(MessageType.CONNECTED, playerId, new ConnectedPayload(playerId, username, stats, recentMatches)));
    }

    private void handleCreateRoom(Object payload) {
        requireConnected();
        if (currentRoomId != null) {
            throw new IllegalStateException("Leave the current room before creating a new one.");
        }
        if (!(payload instanceof CreateRoomRequest request)) {
            throw new IllegalArgumentException("Invalid create room payload.");
        }

        GameRoom room = roomManager.createRoom(this, request.maxPlayers());
        room.broadcastRoomUpdate();
    }

    private void handleJoinRoom(Object payload) {
        requireConnected();
        if (currentRoomId != null) {
            throw new IllegalStateException("You have already joined a room.");
        }
        if (!(payload instanceof JoinRoomRequest request)) {
            throw new IllegalArgumentException("Invalid join room payload.");
        }

        GameRoom room = roomManager.joinRoom(request.roomId(), this);
        room.broadcastRoomUpdate();
    }

    private void handleStartGame() {
        requireConnected();
        if (currentRoomId == null) {
            throw new IllegalStateException("Join a room before starting a game.");
        }

        GameRoom room = roomManager.getRoom(currentRoomId);
        if (room == null) {
            throw new IllegalStateException("Room no longer exists.");
        }

        room.startGame(playerId);
        room.broadcastRoomUpdate();
        room.broadcastGameState();
        room.broadcastTurnUpdate();
        room.scheduleBotTurnIfNeeded();
    }

    private void handleAddBot() {
        requireConnected();
        GameRoom room = requireActiveRoom();
        room.addBot(playerId);
        room.broadcastRoomUpdate();
    }

    private void handleUpdateMaxPlayers(Object payload) {
        if (!(payload instanceof UpdateMaxPlayersRequest request)) {
            throw new IllegalArgumentException("Invalid room size payload.");
        }
        GameRoom room = requireActiveRoom();
        room.updateMaxPlayers(playerId, request.maxPlayers());
        room.broadcastRoomUpdate();
    }

    private void handlePlayCard(Object payload) {
        if (!(payload instanceof PlayCardRequest request)) {
            throw new IllegalArgumentException("Invalid play card payload.");
        }
        GameRoom room = requireActiveRoom();
        room.handlePlayCard(playerId, request.handIndex(), request.chosenColor());
    }

    private void handleDrawCard() {
        GameRoom room = requireActiveRoom();
        room.handleDrawCard(playerId);
    }

    private void handleEndTurn() {
        GameRoom room = requireActiveRoom();
        room.handleEndTurn(playerId);
    }

    private void handleAcceptWildDrawFour() {
        GameRoom room = requireActiveRoom();
        room.handleAcceptWildDrawFour(playerId);
    }

    private void handleTriplePeekChoice(Object payload) {
        if (!(payload instanceof TriplePeekChoiceRequest request)) {
            throw new IllegalArgumentException("Invalid Triple Peek choice payload.");
        }
        GameRoom room = requireActiveRoom();
        room.handleTriplePeekChoice(playerId, request.optionIndex());
    }

    private void handleChallengeWildDrawFour() {
        GameRoom room = requireActiveRoom();
        room.handleChallengeWildDrawFour(playerId);
    }

    private void requireConnected() {
        if (playerId == null || username == null) {
            throw new IllegalStateException("Connect before sending room requests.");
        }
    }

    private void sendError(String message) {
        send(new Message(MessageType.ERROR, playerId, new ErrorPayload(message)));
    }

    private void sendInvalidMove(String message) {
        send(new Message(MessageType.INVALID_MOVE, playerId, new ErrorPayload(message)));
    }

    private GameRoom requireActiveRoom() {
        requireConnected();
        if (currentRoomId == null) {
            throw new IllegalStateException("Join a room before sending game actions.");
        }
        GameRoom room = roomManager.getRoom(currentRoomId);
        if (room == null) {
            throw new IllegalStateException("Room no longer exists.");
        }
        return room;
    }

    private void cleanup() {
        running = false;
        if (currentRoomId != null && playerId != null) {
            roomManager.removePlayer(currentRoomId, playerId);
            currentRoomId = null;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // Nothing else to do during cleanup.
        }
    }
}
