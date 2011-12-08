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
 *
 * See https://code.google.com/apis/analytics/docs/gaJS/gaJSApi.html
 */
public class Analytics
{
    public void trackPageview (String page)
    {
        pushCommand("_trackPageview", page);
    }

    public void trackEvent (String category, String action)
    {
        pushCommand("_trackEvent", category, action);
    }

    public void setCustomVar (int index, String name, String value)
    {
        // Assume all custom vars should be visitor-level
        pushCommand("_setCustomVar", index, name, value, 1);
    }

    protected native boolean pushCommand (Object... command) /*-{
        try {
            $wnd.top._gaq.push(command);
        } catch (_) {
            // Om nom nom
        }
    }-*/;
}
