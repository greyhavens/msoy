//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.PopupCallback;

import client.shell.CShell;

/**
 * Reports a callback error via a popup.
 */
public abstract class InfoCallback<T> extends PopupCallback<T>
{
    /** Used for those times when you just don't care enough to look at the response. */
    public static class NOOP<N> extends InfoCallback<N> {
        public void onSuccess (N value) {
            // noop!
        }
    };

    /**
     * Creates a callback that will display its error in the middle of the page.
     */
    public InfoCallback ()
    {
    }

    /**
     * Creates a callback that will display its error near the supplied widget.
     */
    public InfoCallback (Widget errorNear)
    {
        super(errorNear);
    }

    @Override // from AbstractPopupCallback<T>
    protected String formatError (Throwable cause)
    {
        return CShell.serverError(cause);
    }
}
