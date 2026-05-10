package client.ui;

import client.ClientConnection;
import common.GameSnapshot;
import common.MatchSummary;
import common.NetworkDefaults;
import common.PlayerStats;
import common.RoomSnapshot;
import game.model.Card;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class LobbyFrame extends JFrame implements ClientConnection.Listener {
    private final ClientConnection connection = new ClientConnection();
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField(String.valueOf(NetworkDefaults.DEFAULT_PORT));
    private final JTextField usernameField = new JTextField();
    private final JTextField roomIdField = new JTextField();
    private final JSpinner maxPlayersSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 4, 1));
    private final JButton connectButton = new JButton("Connect");
    private final JButton createRoomButton = new JButton("Create Room");
    private final JButton joinRoomButton = new JButton("Join Room");
    private final JLabel statusLabel = new JLabel("Connect to the server to begin.");
    private final JTextArea statsArea = new JTextArea();

    private GameFrame gameFrame;
    private PlayerStats latestStats;
    private List<MatchSummary> latestRecentMatches = List.of();

    public LobbyFrame() {
        super("UNO - Lobby");
        connection.setListener(this);
        buildUi();
        bindActions();
    }

    @Override
    public void onConnected(String playerId, String username) {
        statusLabel.setText("Connected as " + username + ". You can now create or join a room.");
        createRoomButton.setEnabled(true);
        joinRoomButton.setEnabled(true);
        connectButton.setEnabled(false);
    }

    @Override
    public void onPlayerStatsUpdate(PlayerStats stats, List<MatchSummary> recentMatches) {
        latestStats = stats;
        latestRecentMatches = List.copyOf(recentMatches);
        statsArea.setText(formatStats(stats, recentMatches));
        if (gameFrame != null) {
            gameFrame.showPlayerStats(stats, recentMatches);
        }
    }

    @Override
    public void onRoomUpdate(RoomSnapshot snapshot) {
        statusLabel.setText("Joined room " + snapshot.roomId() + ".");
        showGameFrame();
        gameFrame.showRoomSnapshot(snapshot, connection.getPlayerId());
    }

    @Override
    public void onGameState(GameSnapshot snapshot) {
        showGameFrame();
        gameFrame.showGameSnapshot(snapshot, connection.getPlayerId());
    }

    @Override
    public void onTurnUpdate(String currentTurnPlayerId, String currentTurnUsername) {
        if (gameFrame != null) {
            gameFrame.showTurnUpdate(currentTurnPlayerId, currentTurnUsername, connection.getPlayerId());
        }
    }

    @Override
    public void onTriplePeekOptions(List<Card> options) {
        showGameFrame();
        gameFrame.showTriplePeekOptions(options);
    }

    @Override
    public void onPrivateUpdate(String message) {
        if (gameFrame != null) {
            gameFrame.showPrivateUpdate(message);
        } else {
            JOptionPane.showMessageDialog(this, message, "UNO", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void onInvalidMove(String message) {
        if (gameFrame != null) {
            JOptionPane.showMessageDialog(gameFrame, message, "Invalid Move", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, message, "Invalid Move", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void onGameOver(String winnerPlayerId, String winnerUsername) {
        if (gameFrame != null) {
            gameFrame.showGameOver(winnerPlayerId, winnerUsername, connection.getPlayerId());
            String message = winnerPlayerId.equals(connection.getPlayerId()) ? "You win!" : "Winner: " + winnerUsername;
            JOptionPane.showMessageDialog(gameFrame, message, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void onError(String message) {
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(currentParent(), message, "UNO", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void onDisconnected(String reason) {
        statusLabel.setText(reason);
        createRoomButton.setEnabled(false);
        joinRoomButton.setEnabled(false);
        connectButton.setEnabled(true);

        if (gameFrame != null) {
            gameFrame.dispose();
            gameFrame = null;
        }
        setVisible(true);
        JOptionPane.showMessageDialog(this, reason, "UNO", JOptionPane.INFORMATION_MESSAGE);
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(640, 560));
        setLocationByPlatform(true);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        root.setBackground(new Color(243, 246, 249));
        setContentPane(root);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Multiplayer UNO");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 28f));
        JLabel subtitleLabel = new JLabel("Phase 7 round restart and UI polish");
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 14f));
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitleLabel);

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setOpaque(false);
        addField(form, "Server Host", hostField);
        addField(form, "Server Port", portField);
        addField(form, "Username", usernameField);
        addField(form, "Max Players", maxPlayersSpinner);
        addField(form, "Room ID", roomIdField);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 12, 12));
        buttons.setOpaque(false);
        createRoomButton.setEnabled(false);
        joinRoomButton.setEnabled(false);
        buttons.add(connectButton);
        buttons.add(createRoomButton);
        buttons.add(joinRoomButton);

        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 212, 220)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        statsArea.setEditable(false);
        statsArea.setLineWrap(true);
        statsArea.setWrapStyleWord(true);
        statsArea.setRows(7);
        statsArea.setText("Player stats will appear here after you connect.");
        statsArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 212, 220)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        statsArea.setBackground(Color.WHITE);

        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        footer.add(buttons, BorderLayout.NORTH);
        footer.add(statusLabel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.add(form, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(statsArea), BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(centerPanel, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
    }

    private void bindActions() {
        connectButton.addActionListener(event -> {
            try {
                String username = usernameField.getText().trim();
                if (username.isBlank()) {
                    throw new IllegalArgumentException("Please enter a username.");
                }
                int port = Integer.parseInt(portField.getText().trim());
                connection.connect(hostField.getText().trim(), port, username);
                statusLabel.setText("Connecting...");
            } catch (Exception exception) {
                onError(exception.getMessage());
            }
        });

        createRoomButton.addActionListener(event -> {
            try {
                connection.createRoom((Integer) maxPlayersSpinner.getValue());
            } catch (Exception exception) {
                onError(exception.getMessage());
            }
        });

        joinRoomButton.addActionListener(event -> {
            try {
                String roomId = roomIdField.getText().trim();
                if (roomId.isBlank()) {
                    throw new IllegalArgumentException("Enter the room ID shared by the host.");
                }
                connection.joinRoom(roomId);
            } catch (Exception exception) {
                onError(exception.getMessage());
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                connection.close();
            }
        });
    }

    private void addField(JPanel panel, String labelText, JComponent component) {
        JLabel label = new JLabel(labelText);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        panel.add(label);
        panel.add(component);
    }

    private void showGameFrame() {
        if (gameFrame == null) {
            gameFrame = new GameFrame(connection);
        }
        if (latestStats != null) {
            gameFrame.showPlayerStats(latestStats, latestRecentMatches);
        }
        gameFrame.setVisible(true);
        setVisible(false);
    }

    private String formatStats(PlayerStats stats, List<MatchSummary> recentMatches) {
        if (stats == null) {
            return "Player stats will appear here after you connect.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Profile: ").append(stats.username()).append('\n');
        builder.append("Games Played: ").append(stats.gamesPlayed()).append('\n');
        builder.append("Wins: ").append(stats.wins()).append('\n');
        builder.append(String.format("Win Rate: %.1f%%", stats.winRate())).append('\n');
        builder.append('\n').append("Recent Matches").append('\n');
        if (recentMatches.isEmpty()) {
            builder.append("No completed matches yet.");
            return builder.toString();
        }

        for (MatchSummary match : recentMatches) {
            builder.append("#").append(match.matchId())
                    .append("  ").append(match.result())
                    .append("  Winner: ").append(match.winnerName())
                    .append("  Players: ").append(match.playerCount())
                    .append("  At: ").append(match.playedAt())
                    .append('\n');
        }
        return builder.toString();
    }

    private Component currentParent() {
        return gameFrame != null && gameFrame.isShowing() ? gameFrame : this;
    }
}
