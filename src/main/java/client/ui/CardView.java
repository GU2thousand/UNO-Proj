package client.ui;

import game.model.Card;
import game.model.CardColor;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CardView extends JPanel {
    private static final int CARD_WIDTH = 88;
    private static final int CARD_HEIGHT = 132;

    private Card card;
    private boolean playable;
    private Runnable onClick;
    private boolean interactiveCard;

    public CardView() {
        setOpaque(false);
        setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
    }

    public CardView(Card card) {
        this();
        this.card = card;
    }

    public CardView(Card card, boolean playable, Runnable onClick) {
        this(card);
        interactiveCard = true;
        setInteraction(playable, onClick);
    }

    public void setCard(Card card) {
        this.card = card;
        repaint();
    }

    public void setInteraction(boolean playable, Runnable onClick) {
        this.playable = playable;
        this.onClick = onClick;
        for (var listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
        setCursor(playable && onClick != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        if (playable && onClick != null) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    onClick.run();
                }
            });
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fill = card == null ? new Color(232, 236, 241) : toAwtColor(card.color());
        g2.setColor(fill);
        g2.fillRoundRect(4, 4, CARD_WIDTH - 8, CARD_HEIGHT - 8, 24, 24);

        g2.setColor(playable ? new Color(64, 156, 255) : new Color(36, 42, 49));
        g2.drawRoundRect(4, 4, CARD_WIDTH - 8, CARD_HEIGHT - 8, 24, 24);
        if (playable) {
            g2.setColor(new Color(210, 241, 255));
            g2.drawRoundRect(3, 3, CARD_WIDTH - 6, CARD_HEIGHT - 6, 24, 24);
            g2.setColor(new Color(64, 156, 255));
            g2.drawRoundRect(5, 5, CARD_WIDTH - 10, CARD_HEIGHT - 10, 24, 24);
        } else if (interactiveCard && card != null) {
            g2.setColor(new Color(255, 255, 255, 80));
            g2.fillRoundRect(4, 4, CARD_WIDTH - 8, CARD_HEIGHT - 8, 24, 24);
        }

        String mainText = card == null ? "?" : card.displayLabel();
        String subText = card == null ? "Waiting" : card.color().displayName();

        g2.setColor(card != null && card.color() == CardColor.YELLOW ? new Color(36, 42, 49) : Color.WHITE);
        g2.setFont(bestFitFont(g2, mainText, Font.BOLD, 28f, 14f, CARD_WIDTH - 22));
        drawCenteredString(g2, mainText, 0, 24, CARD_WIDTH, 56);

        g2.setFont(bestFitFont(g2, subText, Font.PLAIN, 13f, 10f, CARD_WIDTH - 18));
        drawCenteredString(g2, subText, 0, 82, CARD_WIDTH, 20);
        g2.dispose();
    }

    private void drawCenteredString(Graphics2D g2, String text, int x, int y, int width, int height) {
        FontMetrics metrics = g2.getFontMetrics();
        int drawX = x + (width - metrics.stringWidth(text)) / 2;
        int drawY = y + ((height - metrics.getHeight()) / 2) + metrics.getAscent();
        g2.drawString(text, drawX, drawY);
    }

    private Font bestFitFont(Graphics2D g2, String text, int style, float preferredSize, float minimumSize, int maxWidth) {
        for (float fontSize = preferredSize; fontSize >= minimumSize; fontSize -= 1f) {
            Font candidate = getFont().deriveFont(style, fontSize);
            if (g2.getFontMetrics(candidate).stringWidth(text) <= maxWidth) {
                return candidate;
            }
        }
        return getFont().deriveFont(style, minimumSize);
    }

    private Color toAwtColor(CardColor color) {
        return switch (color) {
            case RED -> new Color(219, 68, 55);
            case YELLOW -> new Color(244, 180, 0);
            case GREEN -> new Color(15, 157, 88);
            case BLUE -> new Color(66, 133, 244);
            case BLACK -> new Color(52, 58, 64);
        };
    }
}
