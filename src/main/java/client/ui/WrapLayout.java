package client.ui;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= getHgap() + 1;
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            if (targetWidth <= 0) {
                Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
                targetWidth = scrollPane != null ? scrollPane.getWidth() : Integer.MAX_VALUE;
            }

            int horizontalInsets = target.getInsets().left + target.getInsets().right + (getHgap() * 2);
            int maxWidth = Math.max(0, targetWidth - horizontalInsets);
            Dimension size = new Dimension(0, 0);

            int rowWidth = 0;
            int rowHeight = 0;
            int componentCount = target.getComponentCount();
            for (int index = 0; index < componentCount; index++) {
                if (!target.getComponent(index).isVisible()) {
                    continue;
                }

                Dimension dimension = preferred
                        ? target.getComponent(index).getPreferredSize()
                        : target.getComponent(index).getMinimumSize();

                if (rowWidth > 0 && rowWidth + getHgap() + dimension.width > maxWidth) {
                    addRow(size, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }

                if (rowWidth > 0) {
                    rowWidth += getHgap();
                }
                rowWidth += dimension.width;
                rowHeight = Math.max(rowHeight, dimension.height);
            }

            addRow(size, rowWidth, rowHeight);
            size.width += horizontalInsets;
            size.height += target.getInsets().top + target.getInsets().bottom + (getVgap() * 2);
            return size;
        }
    }

    private void addRow(Dimension size, int rowWidth, int rowHeight) {
        size.width = Math.max(size.width, rowWidth);
        if (size.height > 0) {
            size.height += getVgap();
        }
        size.height += rowHeight;
    }
}
