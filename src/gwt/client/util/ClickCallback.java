//
// $Id$

package client.util;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.PromptPopup;

/**
 * Allows one to wire up a button and a service call into one concisely specified little chunk of
 * code. Be sure to call <code>super.onSuccess()</code> and <code>super.onFailure()</code> if you
 * override those methods so that they can automatically reenable the trigger button. NOTE:
 * methods {@link #setConfirmChoices()} and {@link #setConfirmHTML()} are not supported in this
 * subclass and throw an unsupported operation exception.
 */
public abstract class ClickCallback<T> extends com.threerings.gwt.util.ClickCallback<T>
{
    /**
     * Creates a callback for the supplied trigger (the constructor will automatically add this
     * callback to the trigger as a click listener). Failure will automatically be reported.
     */
    public ClickCallback (HasClickHandlers trigger)
    {
        this(trigger, null);
    }

    /**
     * Creates a callback for the supplied trigger (the constructor will automatically add this
     * callback to the trigger as a click listener). Failure will automatically be reported.
     *
     * @param confirmMessage if non-null, a confirm dialog will be popped up when the button is
     * clicked and the service call wil only be made if the user confirms the dialog.
     */
    public ClickCallback (HasClickHandlers trigger, String confirmMessage)
    {
        super(trigger);
        setConfirmText(confirmMessage);
    }

    /**
     * Returns additional information to be added to the confirmation prompt, if we have one. By
     * default we don't.
     */
    protected String getPromptContext ()
    {
        return null;
    }

    protected Widget getErrorNear ()
    {
        return null;
    }

    @Override
    public ClickCallback<T> setConfirmChoices (String confirm, String abort)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClickCallback<T> setConfirmHTML (String message)
    {
        throw new UnsupportedOperationException();
    }

    @Override // from ClickCallback
    protected String formatError (Throwable cause)
    {
        return CShell.serverError(cause);
    }

    @Override // from ClickCallback
    protected void reportFailure (Throwable cause)
    {
        Widget errorNear = getErrorNear();
        if (errorNear != null) {
            if (errorNear instanceof FocusWidget) {
                ((FocusWidget)errorNear).setFocus(true);
            }
            MsoyUI.errorNear(formatError(cause), errorNear);
        } else {
            MsoyUI.error(formatError(cause));
        }
    }

    @Override // from ClickCallback
    protected void displayConfirmPopup ()
    {
        new PromptPopup(_confirmMessage, null) {
            public void onAffirmative () {
                onConfirmed();
            }
            public void onNegative () {
                onAborted();
            }
        }.setContext(getPromptContext()).prompt();
    }
}
