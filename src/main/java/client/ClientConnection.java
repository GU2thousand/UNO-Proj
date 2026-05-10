package client;

import common.GameSnapshot;
import common.Message;
import common.MessageType;
import common.MatchSummary;
import common.PlayerStats;
import common.RoomSnapshot;
import common.payload.ConnectRequest;
import common.payload.ConnectedPayload;
import common.payload.CreateRoomRequest;
import common.payload.ErrorPayload;
import common.payload.GameOverPayload;
import common.payload.JoinRoomRequest;
import common.payload.PrivateUpdatePayload;
import common.payload.PlayerStatsPayload;
import common.payload.PlayCardRequest;
import common.payload.TriplePeekChoiceRequest;
import common.payload.TriplePeekOptionsPayload;
import common.payload.TurnUpdatePayload;
import common.payload.UpdateMaxPlayersRequest;
import game.model.Card;
import game.model.CardColor;

import javax.swing.SwingUtilities;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientConnection {
    public interface Listener {
        void onConnected(String playerId, String username);

        void onRoomUpdate(RoomSnapshot snapshot);

        void onGameState(GameSnapshot snapshot);

        void onTurnUpdate(String currentTurnPlayerId, String currentTurnUsername);

        void onTriplePeekOptions(java.util.List<Card> options);

        void onPlayerStatsUpdate(PlayerStats stats, java.util.List<MatchSummary> recentMatches);

        void onPrivateUpdate(String message);

        void onInvalidMove(String message);

        void onGameOver(String winnerPlayerId, String winnerUsername);

        void onError(String message);

        void onDisconnected(String reason);
    }

    private Listener listener;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Thread readerThread;
    private volatile boolean running;
    private volatile String playerId;
    private volatile String username;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public synchronized void connect(String host, int port, String username) throws IOException {
        if (isConnected()) {
            throw new IllegalStateException("Already connected.");
        }

        this.username = username.trim();
        socket = new Socket(host, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());
        running = true;

        readerThread = new Thread(this::readLoop, "client-message-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        send(new Message(MessageType.CONNECT, null, new ConnectRequest(this.username)));
    }

    public synchronized void createRoom(int maxPlayers) {
        send(new Message(MessageType.CREATE_ROOM, playerId, new CreateRoomRequest(maxPlayers)));
    }

    public synchronized void joinRoom(String roomId) {
        send(new Message(MessageType.JOIN_ROOM, playerId, new JoinRoomRequest(roomId)));
    }

    public synchronized void addBot() {
        send(new Message(MessageType.ADD_BOT, playerId, null));
    }

    public synchronized void updateMaxPlayers(int maxPlayers) {
        send(new Message(MessageType.UPDATE_MAX_PLAYERS, playerId, new UpdateMaxPlayersRequest(maxPlayers)));
    }

    public synchronized void startGame() {
        send(new Message(MessageType.START_GAME, playerId, null));
    }

    public synchronized void playCard(int handIndex, CardColor chosenColor) {
        send(new Message(MessageType.PLAY_CARD, playerId, new PlayCardRequest(handIndex, chosenColor)));
    }

    public synchronized void drawCard() {
        send(new Message(MessageType.DRAW_CARD, playerId, null));
    }

    public synchronized void endTurn() {
        send(new Message(MessageType.END_TURN, playerId, null));
    }

    public synchronized void chooseTriplePeek(int optionIndex) {
        send(new Message(MessageType.TRIPLE_PEEK_CHOICE, playerId, new TriplePeekChoiceRequest(optionIndex)));
    }

    public synchronized void acceptWildDrawFour() {
        send(new Message(MessageType.ACCEPT_WILD_DRAW_FOUR, playerId, null));
    }

    public synchronized void challengeWildDrawFour() {
        send(new Message(MessageType.CHALLENGE_WILD_DRAW_FOUR, playerId, null));
    }

    public synchronized void close() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // Nothing else to do during client shutdown.
        } finally {
            socket = null;
            output = null;
            input = null;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getPlayerId() {
        return playerId;
    }

    private void send(Message message) {
        if (!isConnected() || output == null) {
            throw new IllegalStateException("Not connected to the server.");
        }
        try {
            output.reset();
            output.writeObject(message);
            output.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send message to the server.", exception);
        }
    }

    private void readLoop() {
        try {
            while (running) {
                Object incoming = input.readObject();
                if (incoming instanceof Message message) {
                    handleMessage(message);
                }
            }
        } catch (EOFException ignored) {
            notifyDisconnected("Server disconnected.");
        } catch (Exception exception) {
            if (running) {
                notifyDisconnected("Connection lost: " + exception.getMessage());
            }
        } finally {
            close();
        }
    }

    private void handleMessage(Message message) {
        switch (message.type()) {
            case CONNECTED -> {
                ConnectedPayload payload = (ConnectedPayload) message.payload();
                playerId = payload.playerId();
                dispatch(() -> {
                    if (listener != null) {
                        listener.onConnected(payload.playerId(), payload.username());
                        listener.onPlayerStatsUpdate(payload.stats(), payload.recentMatches());
                    }
                });
            }
            case ROOM_UPDATE -> {
                RoomSnapshot snapshot = (RoomSnapshot) message.payload();
                dispatch(() -> {
                    if (listener != null) {
                        listener.onRoomUpdate(snapshot);
                    }
                });
            }
            case GAME_STATE -> {
                GameSnapshot snapshot = (GameSnapshot) message.payload();
                dispatch(() -> {
                    if (listener != null) {
                        listener.onGameState(snapshot);
                    }
                });
            }
            case TURN_UPDATE -> {
                TurnUpdatePayload payload = (TurnUpdatePayload) message.payload();
                dispatch(() -> {
                    if (listener != null) {
                        listener.onTurnUpdate(payload.currentTurnPlayerId(), payload.currentTurnUsername());
                    }
                });
            }
            case TRIPLE_PEEK_OPTIONS -> {
                TriplePeekOptionsPayload payload = (TriplePeekOptionsPayload) message.payload();
                dispatch(() -> {
                    if (listener != null) {
                        listener.onTriplePeekOptions(payload.options());
                    }
                });
            }
            case PRIVATE_UPDATE -> {
                PrivateUpdatePayload payload = (PrivateUpdatePayload) message.payload();
                dispatch(() -> {
                    if (listener != null) {
                        listener.onPrivateUpdate(payload.message());
                    }
                });
            }
            case PLAYER_STATS_UPDATE -> {
                PlayerStatsPayload payload = (PlayerStatsPayload) message.payload();
                dispatch(() -> {
                    if (listener != null) {
                        listener.onPlayerStatsUpdate(payload.stats(), payload.recentMatches());
                    }
                });
            }
            case INVALID_MOVE -> {
                ErrorPayload payload = (ErrorPayload) message.payload();
                dispatch(() -> {
                    if (listener != null) {
                        listener.onInvalidMove(payload.message());
                    }
                });
            }
            case GAME_OVER -> {
                GameOverPayload payload = (GameOverPayload) message.payload();
                dispatch(() -> {
                    if (listener != null) {
                        listener.onGameOver(payload.winnerPlayerId(), payload.winnerUsername());
                    }
                });
            }
            case ERROR -> {
                ErrorPayload payload = (ErrorPayload) message.payload();
                dispatch(() -> {
                    if (listener != null) {
                        listener.onError(payload.message());
                    }
                });
            }
            default -> dispatch(() -> {
                if (listener != null) {
                    listener.onError("Unknown server message: " + message.type());
                }
            });
        }
    }

    private void dispatch(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }

    private void notifyDisconnected(String reason) {
        dispatch(() -> {
            if (listener != null) {
                listener.onDisconnected(reason);
            }
        });
    }
}
