package persistence;

import common.MatchSummary;
import game.model.PlayerState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MatchRepository {
    private final DatabaseManager databaseManager;
    private final PlayerRepository playerRepository;

    public MatchRepository(DatabaseManager databaseManager, PlayerRepository playerRepository) {
        this.databaseManager = databaseManager;
        this.playerRepository = playerRepository;
    }

    public void recordMatchResult(List<PlayerState> players, String winnerUsername) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long matchId = insertMatch(connection, winnerUsername, players.size());
                insertMatchPlayers(connection, matchId, players, winnerUsername);
                List<String> humanUsernames = players.stream()
                        .filter(player -> !player.isBot())
                        .map(PlayerState::getUsername)
                        .collect(Collectors.toList());
                playerRepository.applyMatchResult(connection, humanUsernames, winnerUsername);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to persist match history.", exception);
        }
    }

    public List<MatchSummary> getRecentMatches(String username, int limit) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT m.id, m.winner_name, m.played_at, m.player_count, mp.result
                     FROM match_players mp
                     JOIN matches m ON m.id = mp.match_id
                     WHERE mp.username = ?
                     ORDER BY m.id DESC
                     LIMIT ?
                     """)) {
            statement.setString(1, username);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MatchSummary> matches = new ArrayList<>();
                while (resultSet.next()) {
                    matches.add(new MatchSummary(
                            resultSet.getLong("id"),
                            resultSet.getString("winner_name"),
                            resultSet.getString("played_at"),
                            resultSet.getInt("player_count"),
                            resultSet.getString("result")
                    ));
                }
                return matches;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load recent match history for " + username + ".", exception);
        }
    }

    private long insertMatch(Connection connection, String winnerUsername, int playerCount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO matches (winner_name, player_count)
                VALUES (?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, winnerUsername);
            statement.setInt(2, playerCount);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("No match id was generated.");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    private void insertMatchPlayers(Connection connection, long matchId, List<PlayerState> players, String winnerUsername) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO match_players (match_id, username, result)
                VALUES (?, ?, ?)
                """)) {
            for (PlayerState player : players) {
                statement.setLong(1, matchId);
                statement.setString(2, player.getUsername());
                statement.setString(3, player.getUsername().equals(winnerUsername) ? "WIN" : "LOSS");
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
