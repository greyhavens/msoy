//
// $Id$

package client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;

import client.shell.CShell;
import client.ui.MsoyUI;

/**
 * Reports a callback error by adding a label to a target panel. The panel will be cleared prior to
 * adding the error.
 */
public abstract class PageCallback<T> implements AsyncCallback<T>
{
    /**
     * Creates a callback that will clear and add an error label to the supplied panel on failure.
     * The panel is left alone in the event of success.
     */
    public PageCallback (Panel panel)
    {
        _panel = panel;
    }

    // from AsyncCallback
    public void onFailure (Throwable cause)
    {
        _panel.clear();
        _panel.add(MsoyUI.createLabel(CShell.serverError(cause), "infoLabel"));
        CShell.log("Service request failed", cause);
    }

    protected Panel _panel;
}
