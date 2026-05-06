package persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final Path DEFAULT_DATABASE_PATH = Path.of("data", "uno.db");
    private final String jdbcUrl;

    public DatabaseManager() {
        this(DEFAULT_DATABASE_PATH);
    }

    public DatabaseManager(Path databasePath) {
        try {
            Files.createDirectories(databasePath.toAbsolutePath().getParent());
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("SQLite JDBC driver is not available on the classpath.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare the database directory.", exception);
        }
        jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
    }

    public void initialize() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT UNIQUE NOT NULL,
                        games_played INTEGER DEFAULT 0,
                        wins INTEGER DEFAULT 0
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS matches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        winner_name TEXT,
                        played_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        player_count INTEGER
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS match_players (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        match_id INTEGER NOT NULL,
                        username TEXT NOT NULL,
                        result TEXT NOT NULL
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize the SQLite schema.", exception);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}
