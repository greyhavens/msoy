//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

/** 
 * An interface to display passive notifications.
 */
public interface PassiveNotifier
{
    /** Display an info level message */
    public void showInfo (String message);

    /** Display an error level message */
    public void showError (String message);
}
