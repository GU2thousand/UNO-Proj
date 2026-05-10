package client;

import client.ui.LobbyFrame;

import javax.swing.SwingUtilities;

public final class ClientMain {
    private ClientMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LobbyFrame lobbyFrame = new LobbyFrame();
            lobbyFrame.setVisible(true);
        });
    }
}
