package server;

import persistence.MatchRepository;
import persistence.PlayerRepository;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RoomManager {
    private static final String ROOM_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    public RoomManager(PlayerRepository playerRepository, MatchRepository matchRepository) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
    }

    public synchronized GameRoom createRoom(ClientHandler host, int maxPlayers) {
        if (maxPlayers < 2 || maxPlayers > 4) {
            throw new IllegalArgumentException("Room size must be between 2 and 4.");
        }

        String roomId;
        do {
            roomId = nextRoomId();
        } while (rooms.containsKey(roomId));

        GameRoom room = new GameRoom(roomId, maxPlayers, host.getPlayerId(), playerRepository, matchRepository);
        rooms.put(roomId, room);
        room.addPlayer(host);
        return room;
    }

    public GameRoom joinRoom(String roomId, ClientHandler player) {
        String normalizedRoomId = normalizeRoomId(roomId);
        GameRoom room = rooms.get(normalizedRoomId);
        if (room == null) {
            throw new IllegalArgumentException("Room " + normalizedRoomId + " does not exist.");
        }
        room.addPlayer(player);
        return room;
    }

    public GameRoom getRoom(String roomId) {
        if (roomId == null) {
            return null;
        }
        return rooms.get(normalizeRoomId(roomId));
    }

    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }

    public MatchRepository getMatchRepository() {
        return matchRepository;
    }

    public void removePlayer(String roomId, String playerId) {
        GameRoom room = getRoom(roomId);
        if (room == null) {
            return;
        }
        room.removePlayer(playerId);
        if (room.isEmpty()) {
            room.shutdown();
            rooms.remove(room.getRoomId());
            return;
        }
        room.broadcastRoomUpdate();
        if (room.isStarted()) {
            room.broadcastGameState();
            room.scheduleBotTurnIfNeeded();
        }
    }

    private String normalizeRoomId(String roomId) {
        return roomId.trim().toUpperCase(Locale.ROOT);
    }

    private String nextRoomId() {
        StringBuilder builder = new StringBuilder(6);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            builder.append(ROOM_ALPHABET.charAt(random.nextInt(ROOM_ALPHABET.length())));
        }
        return builder.toString();
    }
}
