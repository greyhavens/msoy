//
// $Id$

package client.frame;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.user.client.Timer;

import client.shell.CShell;

/**
 * Handles communication with Google Analytics. This assumes Whirled has access to window.top and is
 * not being embedded in a 3rd party site (such as a FB canvas app).
 */
public class Analytics
{
    /**
     * Reports a page view to Google Analytics.
     */
    protected native boolean report (String page) /*-{
        try {
            $wnd.top._gaq.push(["_trackPageview", page]);
        } catch (_) {
            // Om nom nom
        }
    }-*/;
}
