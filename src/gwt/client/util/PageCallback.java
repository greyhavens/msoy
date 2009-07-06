//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Panel;

import com.threerings.gwt.util.AbstractPanelCallback;

import client.shell.CShell;

/**
 * Reports a callback error by adding a label to a target panel. The panel will be cleared prior to
 * adding the error.
 */
public abstract class PageCallback<T> extends AbstractPanelCallback<T>
{
    /**
     * Creates a callback that will clear and add an error label to the supplied panel on failure.
     * The panel is left alone in the event of success.
     */
    public PageCallback (Panel panel)
    {
        super(panel);
    }

    @Override // from AbstractPanelCallback<T>
    protected String formatError (Throwable cause)
    {
        return CShell.serverError(cause);
    }
}
