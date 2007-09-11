//
// $Id$
package com.threerings.msoy.swiftly.client.view;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JProgressBar;
import javax.swing.Timer;

/**
 * Simple progress bar to be used by the Swiftly context.
 */
public class SimpleProgressBar extends JProgressBar
    implements ActionListener
{
    public SimpleProgressBar ()
    {
        _timer = new Timer(TIMER_INTERVAL, this);

        setVisible(false);
        setMaximumSize(new Dimension(120, 15));
    }

    // from ActionListener
    public void actionPerformed (ActionEvent e)
    {
        long value = System.currentTimeMillis() - _startTime;
        setValue((int)value);
        if (getPercentComplete() >= 100) {
            stopProgress();
        }
    }

    /**
     * Display a progress bar for a task taking the number of milliseconds supplied.
     */
    public void showProgress (int time)
    {
        // if the time is less than our timer interval, no point in showing the progress bar
        if (time < TIMER_INTERVAL) {
            return;
        }

        setVisible(true);
        setMaximum(time);
        _startTime = System.currentTimeMillis();
        _timer.restart();
    }

    /**
     * Stop displaying the progress bar.
     */
    public void stopProgress ()
    {
        _timer.stop();
        setVisible(false);
        setValue(0);
    }

    /** How often the progress bar updates */
    protected static final int TIMER_INTERVAL = 1000;

    protected final Timer _timer;
    protected long _startTime;
}
