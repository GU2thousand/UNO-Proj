package persistence;

import common.PlayerStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PlayerRepository {
    private final DatabaseManager databaseManager;

    public PlayerRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void createPlayerIfNotExists(String username) {
        try (Connection connection = databaseManager.getConnection()) {
            createPlayerIfNotExists(connection, username);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create or load player profile for " + username + ".", exception);
        }
    }

    public PlayerStats getPlayerStats(String username) {
        try (Connection connection = databaseManager.getConnection()) {
            createPlayerIfNotExists(connection, username);
            return getPlayerStats(connection, username);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load player statistics for " + username + ".", exception);
        }
    }

    void createPlayerIfNotExists(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO players (username)
                VALUES (?)
                ON CONFLICT(username) DO NOTHING
                """)) {
            statement.setString(1, username);
            statement.executeUpdate();
        }
    }

    PlayerStats getPlayerStats(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT username, games_played, wins
                FROM players
                WHERE username = ?
                """)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new PlayerStats(username, 0, 0, 0.0);
                }

                int gamesPlayed = resultSet.getInt("games_played");
                int wins = resultSet.getInt("wins");
                double winRate = gamesPlayed == 0 ? 0.0 : (wins * 100.0) / gamesPlayed;
                return new PlayerStats(resultSet.getString("username"), gamesPlayed, wins, winRate);
            }
        }
    }

    void applyMatchResult(Connection connection, List<String> usernames, String winnerUsername) throws SQLException {
        for (String username : usernames) {
            createPlayerIfNotExists(connection, username);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE players
                    SET games_played = games_played + 1,
                        wins = wins + CASE WHEN username = ? THEN 1 ELSE 0 END
                    WHERE username = ?
                    """)) {
                statement.setString(1, winnerUsername);
                statement.setString(2, username);
                statement.executeUpdate();
            }
        }
    }
}
