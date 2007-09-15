//
// $Id$

package com.threerings.msoy.swiftly.client.view;

/**
 * Display visual feedback for long running processes.
 */
public interface ProgressBar
{
    /**
     * Display a progress bar for a task taking the number of milliseconds supplied.
     */
    public abstract void showProgress (int time);

    /**
     * Stop displaying the progress bar.
     */
    public abstract void stopProgress ();
}