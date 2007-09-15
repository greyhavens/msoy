//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.LinkedList;

import javax.swing.JLayeredPane;
import javax.swing.Timer;
import javax.swing.SwingConstants;

import com.samskivert.swing.LabelStyleConstants;
import com.samskivert.swing.MultiLineLabel;
import com.samskivert.swing.util.SwingUtil;
import com.threerings.msoy.swiftly.client.controller.PassiveNotifier;

/**
 * Displays translucent labels which remove themselves after an interval.
 */
public class GrowlStyleNotifier implements PassiveNotifier
{
    public GrowlStyleNotifier (JLayeredPane layeredPane)
    {
        _layeredPane = layeredPane;
    }

    // from PassiveNotifier
    public void showInfo (String message)
    {
        addLabel(message, Level.INFO);
    }

    // from PassiveNotifier
    public void showError (String message)
    {
        addLabel(message, Level.ERROR);
    }

    private void addLabel (String message, Level level)
    {
        NotificationLabel label = new NotificationLabel(message, level, this);
        // show the label for 5 seconds
        Timer timer = new Timer(5000, label);
        timer.setRepeats(false);
        timer.start();

        _layeredPane.add(label, NOTIFICATION_LAYER);
        _displayedLabels.addFirst(label);
        drawLabels();
    }

    private void removeLabel (NotificationLabel label)
    {
        _layeredPane.remove(label);
        _displayedLabels.remove(label);
        drawLabels();
    }

    private void drawLabels ()
    {
        int nextY = _layeredPane.getHeight();
        for (NotificationLabel label : _displayedLabels) {
            // need to force the size in order for the layered pane to draw the label
            label.setSize(LABEL_WIDTH, label.getPreferredSize().height);
            int labelX = _layeredPane.getWidth() - label.getWidth();
            int labelY = nextY - label.getHeight();
            label.setLocation(labelX, labelY);
            nextY = labelY - LABEL_SPACE;
        }
        SwingUtil.refresh(_layeredPane);
    }

    private static class NotificationLabel extends MultiLineLabel
        implements ActionListener, MouseListener
    {
        public NotificationLabel (String message, Level level, GrowlStyleNotifier notifier)
        {
            super(message, SwingConstants.CENTER, SwingConstants.HORIZONTAL, LABEL_WIDTH);
            _notifier = notifier;
            _bgColor = level.color;

            setOpaque(true);
            setForeground(Color.black);
            setFont(LABEL_FONT);
            setStyle(LabelStyleConstants.BOLD);
            setAntiAliased(true);
            addMouseListener(this);
        }

        @Override // from MultiLineLabel
        public void paintComponent (Graphics g)
        {
            g.setColor(_bgColor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            super.paintComponent(g);
        }

        // from ActionListener
        public void actionPerformed (ActionEvent evt)
        {
            _notifier.removeLabel(this);
        }

        // from MouseListener
        public void mouseClicked (MouseEvent e) {
            _notifier.removeLabel(this);
        }
        public void mouseEntered (MouseEvent e) {
            // TODO: draw a border
        }
        public void mouseExited (MouseEvent e) {
            // TODO: clear the border
        }
        public void mousePressed (MouseEvent e) {
            // nada
        }
        public void mouseReleased (MouseEvent e) {
            // nada
        }

        private static final Font LABEL_FONT = new Font("Times", Font.PLAIN, 13);

        private final GrowlStyleNotifier _notifier;
        private final Color _bgColor;
    }

    /** Constrain the notification label to this width. */
    private static final int LABEL_WIDTH = 250;

    /** Use a unique value for the JLayeredPane layer to avoid conflicts with other windows. */
    private static final Integer NOTIFICATION_LAYER = new Integer(23);

    /** The spacing between the labels */
    private static final int LABEL_SPACE = 4;

    private final JLayeredPane _layeredPane;
    private static final LinkedList<NotificationLabel> _displayedLabels =
        new LinkedList<NotificationLabel>();

    private static enum Level {
        INFO (new Color(0.5f, 0.5f, 0.5f, 0.8f)),
        ERROR (new Color(1.0f, 0.0f, 0.0f, 0.8f));

        public Color color;

        Level (Color color) {
           this.color = color;
        }
    }
}
