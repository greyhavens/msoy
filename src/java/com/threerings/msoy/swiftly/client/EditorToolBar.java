//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class EditorToolBar extends JToolBar
    implements AccessControlListener
{
    public EditorToolBar (ProjectRoomController ctrl, SwiftlyContext ctx, SwiftlyEditor editor)
    {
        _ctrl = ctrl;
        _ctx = ctx;
        _editor = editor;

        // TODO tooltips with keyboard shortcuts
        add(_buildButton = createButton(_ctrl.buildAction, BUILD_ICON));
        add(_buildExportButton = createButton(_ctrl.buildExportAction, PREVIEW_ICON));
        add(_consoleButton = createButton(_editor.createShowConsoleAction(), CONSOLE_ICON));

        _progress = new JProgressBar();
        _progress.setVisible(false);
        _progress.setMinimum(0);
        add(Box.createHorizontalGlue());
        add(_progress);

        setFloatable(false);
    }

    // from AccessControlListener
    public void writeAccessGranted ()
    {
        _buildButton.setEnabled(true);
        _buildExportButton.setEnabled(true);
    }

    // from AccessControlListener
    public void readOnlyAccessGranted ()
    {
        _buildButton.setEnabled(false);
        _buildExportButton.setEnabled(false);
    }

    /**
     * Display a progress bar for a task taking the number of milliseconds supplied.
     */
    public void showProgress (final int time)
    {
        // if the time is less than our timer interval, no point in showing the progress bar
        if (time < TIMER_INTERVAL) {
            return;
        }

        _progress.setVisible(true);

        // if time is 0, show an indeterminate progress bar as we have no idea how long
        // this task will take
        if (time == 0) {
            _progress.setIndeterminate(true);
            return;
        }

        _progress.setMaximum(time);
        _progress.setIndeterminate(false);
        _timer = new Timer(TIMER_INTERVAL, new ActionListener() {
            public void actionPerformed(ActionEvent A) {
                int value = (time / (time / TIMER_INTERVAL)) * _count;
                _progress.setValue(value);
                _count++;
                if (_progress.getPercentComplete() >= 100) {
                    stopProgress();
                }
            }
            protected int _count = 1;
        });
        _timer.start();
    }

    /**
     * Stop displaying the progress bar.
     */
    public void stopProgress ()
    {
        if (_timer != null) {
            _timer.stop();
        }
        _progress.setVisible(false);
        _progress.setValue(0);
    }

    protected JButton createButton (Action action, String icon)
    {
        JButton button = new JButton(action);
        button.setIcon(new ImageIcon(getClass().getResource(icon)));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        // TODO: button.setMargin(new Insets(15, 0, 0, 0));
        // text below and centered on icons
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        return button;
    }

    /** The location of various icons */
    protected static final String BUILD_ICON = "/rsrc/icons/swiftly/build.png";
    protected static final String PREVIEW_ICON = "/rsrc/icons/swiftly/preview.png";
    protected static final String CONSOLE_ICON = "/rsrc/icons/swiftly/console.png";

    protected static final int TIMER_INTERVAL = 1000;

    protected ProjectRoomController _ctrl;
    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;

    protected JButton _buildButton;
    protected JButton _buildExportButton;
    protected JButton _consoleButton;
    protected JProgressBar _progress;
    protected Timer _timer;
}
