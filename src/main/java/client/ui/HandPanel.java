package client.ui;

import game.model.Card;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class HandPanel extends JPanel implements Scrollable {
    public HandPanel() {
        super(new WrapLayout(FlowLayout.LEFT, 12, 12));
        setOpaque(true);
        setBackground(Color.WHITE);
    }

    public void setCards(List<Card> cards, List<Integer> playableCardIndexes, Consumer<Integer> onCardClick) {
        removeAll();
        Set<Integer> playableIndexSet = Set.copyOf(playableCardIndexes);
        for (int index = 0; index < cards.size(); index++) {
            int cardIndex = index;
            boolean playable = playableIndexSet.contains(index);
            Runnable clickAction = playable && onCardClick != null ? () -> onCardClick.accept(cardIndex) : null;
            add(new CardView(cards.get(index), playable, clickAction));
        }
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height - 24 : visibleRect.width - 24;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
