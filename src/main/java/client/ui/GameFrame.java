package client.ui;

import client.ClientConnection;
import common.GameSnapshot;
import common.MatchSummary;
import common.PlayerInfo;
import common.PlayerStats;
import common.RoomSnapshot;
import game.model.Card;
import game.model.CardColor;
import game.model.CardType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public class GameFrame extends JFrame {
    private final ClientConnection connection;
    private final JLabel roomLabel = new JLabel("Room: -");
    private final JLabel statusLabel = new JLabel("Waiting for room updates...");
    private final JLabel turnLabel = new JLabel("Turn: -");
    private final JTextArea playersArea = new JTextArea();
    private final JTextArea statsArea = new JTextArea();
    private final JButton startButton = new JButton("Start Game");
    private final JButton roomSizeButton = new JButton("Room Size");
    private final JButton addBotButton = new JButton("Add Bot");
    private final JButton drawButton = new JButton("Draw Card");
    private final JButton acceptWildDrawFourButton = new JButton("Accept +4");
    private final JButton challengeWildDrawFourButton = new JButton("Challenge +4");
    private final CardView discardCardView = new CardView();
    private final HandPanel handPanel = new HandPanel();
    private GameSnapshot latestSnapshot;
    private String currentPlayerId;
    private boolean currentUserIsHost;

    public GameFrame(ClientConnection connection) {
        super("UNO - Game");
        this.connection = connection;
        buildUi();
    }

    public void showRoomSnapshot(RoomSnapshot snapshot, String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
        currentUserIsHost = snapshot.hostPlayerId().equals(currentPlayerId);
        roomLabel.setText("Room: " + snapshot.roomId() + " (" + snapshot.players().size() + "/" + snapshot.maxPlayers() + ")");
        statusLabel.setText(snapshot.started() ? "Game started." : "Waiting for the host to start or add bots.");
        if (!snapshot.started()) {
            updateTurnLabel("Waiting to start", false);
            discardCardView.setCard(null);
            handPanel.setCards(List.of(), List.of(), null);
            drawButton.setEnabled(false);
            setWildDrawFourActionVisibility(false);
            startButton.setText("Start Game");
        }
        roomSizeButton.setVisible(!snapshot.started());
        roomSizeButton.setEnabled(!snapshot.started() && snapshot.hostPlayerId().equals(currentPlayerId));
        roomSizeButton.putClientProperty("room.maxPlayers", snapshot.maxPlayers());
        addBotButton.setEnabled(!snapshot.started()
                && snapshot.hostPlayerId().equals(currentPlayerId)
                && snapshot.players().size() < snapshot.maxPlayers());
        startButton.setVisible(!snapshot.started());
        startButton.setEnabled(!snapshot.started()
                && snapshot.hostPlayerId().equals(currentPlayerId)
                && snapshot.players().size() == snapshot.maxPlayers());
        renderPlayers(snapshot.players(), null, snapshot.hostPlayerId(), snapshot.started());
    }

    public void showGameSnapshot(GameSnapshot snapshot, String currentPlayerId) {
        latestSnapshot = snapshot;
        this.currentPlayerId = currentPlayerId;
        statusLabel.setText(buildStatusText(snapshot));
        updateTurnLabel(snapshot.currentTurnUsername(), snapshot.currentTurnPlayerId().equals(currentPlayerId));
        discardCardView.setCard(snapshot.topDiscard());
        handPanel.setCards(snapshot.yourHand(), snapshot.playableCardIndexes(), this::handleCardClick);
        addBotButton.setEnabled(false);
        roomSizeButton.setVisible(false);
        startButton.setVisible(false);
        startButton.setEnabled(false);
        drawButton.setEnabled(snapshot.canDraw());
        setWildDrawFourActionVisibility(snapshot.canAcceptWildDrawFour());
        renderPlayers(snapshot.players(), snapshot.currentTurnPlayerId(), null, true);
    }

    public void showTriplePeekOptions(List<Card> options) {
        Integer choice = promptForTriplePeekChoice(options);
        if (choice != null) {
            connection.chooseTriplePeek(choice);
        }
    }

    public void showPrivateUpdate(String message) {
        JOptionPane.showMessageDialog(this, message, "UNO", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showPlayerStats(PlayerStats stats, List<MatchSummary> recentMatches) {
        statsArea.setText(formatStats(stats, recentMatches));
    }

    public void showTurnUpdate(String currentTurnPlayerId, String currentTurnUsername, String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
        updateTurnLabel(currentTurnUsername, currentTurnPlayerId.equals(currentPlayerId));
    }

    public void showGameOver(String winnerPlayerId, String winnerUsername, String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
        statusLabel.setText(winnerPlayerId.equals(currentPlayerId) ? "You win!" : "Winner: " + winnerUsername);
        drawButton.setEnabled(false);
        setWildDrawFourActionVisibility(false);
        addBotButton.setEnabled(false);
        roomSizeButton.setVisible(false);
        startButton.setText("New Round");
        startButton.setVisible(true);
        startButton.setEnabled(currentUserIsHost);
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 680));
        setLocationByPlatform(true);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(243, 246, 249));
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        roomLabel.setFont(roomLabel.getFont().deriveFont(Font.BOLD, 22f));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 14f));
        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleBlock.add(roomLabel);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(statusLabel);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(turnLabel);

        startButton.addActionListener(event -> connection.startGame());
        roomSizeButton.addActionListener(event -> promptForRoomSizeUpdate());
        addBotButton.addActionListener(event -> connection.addBot());
        drawButton.addActionListener(event -> connection.drawCard());
        acceptWildDrawFourButton.addActionListener(event -> connection.acceptWildDrawFour());
        challengeWildDrawFourButton.addActionListener(event -> connection.challengeWildDrawFour());
        startButton.setVisible(false);
        roomSizeButton.setVisible(false);
        addBotButton.setEnabled(false);
        drawButton.setEnabled(false);
        setWildDrawFourActionVisibility(false);

        JPanel actionButtons = new JPanel();
        actionButtons.setOpaque(false);
        actionButtons.add(addBotButton);
        actionButtons.add(drawButton);
        actionButtons.add(acceptWildDrawFourButton);
        actionButtons.add(challengeWildDrawFourButton);

        header.add(titleBlock, BorderLayout.CENTER);
        JPanel headerActions = new JPanel(new BorderLayout(8, 8));
        headerActions.setOpaque(false);
        JPanel preStartButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        preStartButtons.setOpaque(false);
        preStartButtons.add(roomSizeButton);
        preStartButtons.add(startButton);
        headerActions.add(preStartButtons, BorderLayout.NORTH);
        headerActions.add(actionButtons, BorderLayout.SOUTH);
        header.add(headerActions, BorderLayout.EAST);

        JPanel rightPanel = new JPanel(new BorderLayout(12, 12));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(320, 0));

        JLabel playersLabel = new JLabel("Players");
        playersLabel.setFont(playersLabel.getFont().deriveFont(Font.BOLD, 16f));
        playersArea.setEditable(false);
        playersArea.setOpaque(false);
        playersArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        playersArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 212, 220)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        statsArea.setEditable(false);
        statsArea.setLineWrap(true);
        statsArea.setWrapStyleWord(true);
        statsArea.setRows(8);
        statsArea.setText("Your player stats will appear here.");
        statsArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 212, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        statsArea.setBackground(Color.WHITE);

        JPanel discardPanel = new JPanel();
        discardPanel.setOpaque(false);
        discardPanel.setLayout(new BoxLayout(discardPanel, BoxLayout.Y_AXIS));
        JLabel discardLabel = new JLabel("Discard Pile");
        discardLabel.setAlignmentX(CENTER_ALIGNMENT);
        discardLabel.setFont(discardLabel.getFont().deriveFont(Font.BOLD, 16f));
        JPanel discardCardHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        discardCardHolder.setOpaque(false);
        discardCardHolder.add(discardCardView);
        discardPanel.add(discardLabel);
        discardPanel.add(Box.createVerticalStrut(12));
        discardPanel.add(discardCardHolder);

        JPanel sideStack = new JPanel();
        sideStack.setOpaque(false);
        sideStack.setLayout(new BoxLayout(sideStack, BoxLayout.Y_AXIS));
        sideStack.add(playersLabel);
        sideStack.add(Box.createVerticalStrut(8));
        sideStack.add(playersArea);
        sideStack.add(Box.createVerticalStrut(12));
        JLabel statsLabel = new JLabel("Profile");
        statsLabel.setFont(statsLabel.getFont().deriveFont(Font.BOLD, 16f));
        sideStack.add(statsLabel);
        sideStack.add(Box.createVerticalStrut(8));
        sideStack.add(new JScrollPane(statsArea));

        rightPanel.add(sideStack, BorderLayout.CENTER);
        rightPanel.add(discardPanel, BorderLayout.SOUTH);

        JPanel handContainer = new JPanel(new BorderLayout());
        handContainer.setOpaque(false);
        JLabel handLabel = new JLabel("Your Hand");
        handLabel.setFont(handLabel.getFont().deriveFont(Font.BOLD, 18f));
        JScrollPane handScrollPane = new JScrollPane(handPanel);
        handScrollPane.setBorder(BorderFactory.createLineBorder(new Color(205, 212, 220)));
        handScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        handScrollPane.getViewport().setBackground(Color.WHITE);
        handContainer.add(handLabel, BorderLayout.NORTH);
        handContainer.add(handScrollPane, BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(handContainer, BorderLayout.CENTER);
        root.add(rightPanel, BorderLayout.EAST);
    }

    private String buildStatusText(GameSnapshot snapshot) {
        if (snapshot.finished()) {
            return "Game over. Winner: " + snapshot.winnerUsername() + ". Active color was " + snapshot.currentColor().displayName() + ".";
        }
        if (snapshot.triplePeekChoicePending()) {
            if (snapshot.yourTurn()) {
                return "Triple Peek is active. Choose one of the private options you received. Active color: "
                        + snapshot.currentColor().displayName() + ".";
            }
            return "Waiting for " + snapshot.triplePeekPlayerUsername() + " to finish Triple Peek. Active color: "
                    + snapshot.currentColor().displayName() + ".";
        }
        if (!snapshot.yourTurn()) {
            if (snapshot.wildDrawFourChallengePending()) {
                return "Waiting for " + snapshot.currentTurnUsername() + " to accept or challenge "
                        + snapshot.wildDrawFourPlayerUsername() + "'s +4. Active color: "
                        + snapshot.currentColor().displayName() + ".";
            }
            if (snapshot.pendingDrawCount() > 0) {
                return "Waiting for " + snapshot.currentTurnUsername() + " to respond to +" + snapshot.pendingDrawCount()
                        + ". Active color: " + snapshot.currentColor().displayName() + ".";
            }
            return "Waiting for " + snapshot.currentTurnUsername() + " to play. Active color: " + snapshot.currentColor().displayName() + ".";
        }
        if (snapshot.wildDrawFourChallengePending()) {
            return snapshot.wildDrawFourPlayerUsername() + " played +4. Accept it or challenge it. Active color: "
                    + snapshot.currentColor().displayName() + ".";
        }
        if (snapshot.pendingDrawCount() > 0) {
            return "Your turn. Stack a +2 or draw +" + snapshot.pendingDrawCount()
                    + ". Active color: " + snapshot.currentColor().displayName() + ".";
        }
        if (snapshot.canDraw() && hasCustomActionOptions(snapshot)) {
            return "No ordinary playable cards. Use Group Draw / Triple Peek if you want, or draw one. Active color: "
                    + snapshot.currentColor().displayName() + ".";
        }
        if (snapshot.canDraw()) {
            return "No playable cards. Draw one card. If it is playable, it will be played automatically. Active color: "
                    + snapshot.currentColor().displayName() + ".";
        }
        if (!snapshot.playableCardIndexes().isEmpty()) {
            return "Your turn. Play a highlighted card. Active color: " + snapshot.currentColor().displayName() + ".";
        }
        return "Your turn. Active color: " + snapshot.currentColor().displayName() + ".";
    }

    private void handleCardClick(int handIndex) {
        if (latestSnapshot == null || handIndex < 0 || handIndex >= latestSnapshot.yourHand().size()) {
            return;
        }

        Card card = latestSnapshot.yourHand().get(handIndex);
        CardColor chosenColor = null;
        if (card.type() == CardType.WILD || card.type() == CardType.WILD_DRAW_FOUR) {
            chosenColor = promptForColor();
            if (chosenColor == null) {
                return;
            }
        }

        connection.playCard(handIndex, chosenColor);
    }

    private CardColor promptForColor() {
        CardColor[] colors = {CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE};
        String[] options = {"Red", "Yellow", "Green", "Blue"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose the active color:",
                "Pick a Color",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice < 0 || choice >= colors.length) {
            return null;
        }
        return colors[choice];
    }

    private Integer promptForTriplePeekChoice(List<Card> options) {
        if (options.isEmpty()) {
            return null;
        }

        List<String> labels = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            labels.add((index + 1) + ". " + options.get(index));
        }

        while (isDisplayable()) {
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "Triple Peek: choose 1 card to keep.",
                    "Triple Peek",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    labels.toArray(String[]::new),
                    labels.get(0)
            );
            if (choice >= 0 && choice < options.size()) {
                return choice;
            }
        }
        return null;
    }

    private boolean hasCustomActionOptions(GameSnapshot snapshot) {
        for (Integer index : snapshot.playableCardIndexes()) {
            CardType type = snapshot.yourHand().get(index).type();
            if (type == CardType.GROUP_DRAW || type == CardType.TRIPLE_PEEK) {
                return true;
            }
        }
        return false;
    }

    private void updateTurnLabel(String currentTurnUsername, boolean yourTurn) {
        if (yourTurn) {
            turnLabel.setText("<html>Turn: " + currentTurnUsername + " <font color='#c62828'>(Your Turn!)</font></html>");
        } else {
            turnLabel.setText("Turn: " + currentTurnUsername);
        }
    }

    private void setWildDrawFourActionVisibility(boolean visible) {
        acceptWildDrawFourButton.setVisible(visible);
        challengeWildDrawFourButton.setVisible(visible);
        acceptWildDrawFourButton.setEnabled(visible);
        challengeWildDrawFourButton.setEnabled(visible);
    }

    private void promptForRoomSizeUpdate() {
        Object value = roomSizeButton.getClientProperty("room.maxPlayers");
        int currentMaxPlayers = value instanceof Integer maxPlayers ? maxPlayers : 2;
        Integer[] options = {2, 3, 4};
        Integer selected = (Integer) JOptionPane.showInputDialog(
                this,
                "Choose the maximum number of players for this room:",
                "Room Size",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                currentMaxPlayers
        );
        if (selected != null && selected != currentMaxPlayers) {
            connection.updateMaxPlayers(selected);
        }
    }

    private void renderPlayers(List<PlayerInfo> players, String currentTurnPlayerId, String hostPlayerId, boolean started) {
        StringBuilder builder = new StringBuilder();
        for (PlayerInfo player : players) {
            if (currentTurnPlayerId != null && currentTurnPlayerId.equals(player.playerId())) {
                builder.append("-> ");
            } else {
                builder.append("   ");
            }
            builder.append(player.username());
            if (hostPlayerId != null && hostPlayerId.equals(player.playerId())) {
                builder.append(" [Host]");
            }
            if (player.bot()) {
                builder.append(" [Bot]");
            }
            if (started) {
                builder.append(" - ").append(player.cardCount()).append(" cards");
            }
            builder.append('\n');
        }
        playersArea.setText(builder.toString());
    }

    private String formatStats(PlayerStats stats, List<MatchSummary> recentMatches) {
        if (stats == null) {
            return "Your player stats will appear here.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Games: ").append(stats.gamesPlayed()).append('\n');
        builder.append("Wins: ").append(stats.wins()).append('\n');
        builder.append(String.format("Win Rate: %.1f%%", stats.winRate())).append('\n');
        builder.append('\n').append("Recent").append('\n');
        if (recentMatches.isEmpty()) {
            builder.append("No completed matches yet.");
            return builder.toString();
        }

        for (MatchSummary match : recentMatches) {
            builder.append("#").append(match.matchId())
                    .append(" ").append(match.result())
                    .append(" / ").append(match.winnerName())
                    .append(" / ").append(match.playedAt())
                    .append('\n');
        }
        return builder.toString();
    }
}
