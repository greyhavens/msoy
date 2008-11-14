//
// $Id$

package client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.PromptPopup;

/**
 * Allows one to wire up a button and a service call into one concisely specified little chunk of
 * code. Be sure to call <code>super.onSuccess()</code> and <code>super.onFailure()</code> if you
 * override those methods so that they can automatically reenable the trigger button.
 */
public abstract class ClickCallback<T>
    implements AsyncCallback<T>
{
    /**
     * Creates a callback for the supplied trigger (the constructor will automatically add this
     * callback to the trigger as a click listener). Failure will automatically be reported.
     */
    public ClickCallback (SourcesClickEvents trigger)
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
    public ClickCallback (SourcesClickEvents trigger, String confirmMessage)
    {
        _trigger = trigger;
        _trigger.addClickListener(_onClick);
        if (_trigger instanceof Label) {
            // make sure to add our style, but don't doubly add it if it's already added
            ((Label)_trigger).removeStyleName("actionLabel");
            ((Label)_trigger).addStyleName("actionLabel");
        }
        _confirmMessage = confirmMessage;
    }

    // from interface AsyncCallback
    public void onSuccess (T result)
    {
        setEnabled(gotResult(result));
    }

    // from interface AsyncCallback
    public void onFailure (Throwable cause)
    {
        CShell.log("Callback failure [for=" + _trigger + "]", cause);
        setEnabled(true);
        reportFailure(cause);
    }

    /**
     * This method is called when the trigger button is clicked. Pass <code>this</code> as the
     * {@link AsyncCallback} to a service method. Return true from this method if a service request
     * was initiated and the button that triggered it should be disabled.
     */
    protected abstract boolean callService ();

    /**
     * This method will be called when the service returns successfully. Return true if the trigger
     * should now be reenabled, false to leave it disabled.
     */
    protected abstract boolean gotResult (T result);

    /**
     * Reports a failure to the user. Derived classes can override this and customize the way
     * failure is reported if they so desire (it is safe not to call super.reportFailure() in that
     * case).
     */
    protected void reportFailure (Throwable cause)
    {
        Widget errorNear = getErrorNear();
        if (errorNear != null) {
            if (errorNear instanceof FocusWidget) {
                ((FocusWidget)errorNear).setFocus(true);
            }
            MsoyUI.errorNear(convertError(cause), errorNear);
        } else {
            MsoyUI.error(convertError(cause));
        }
    }

    /**
     * Returns additional information to be added to the confirmation prompt, if we have one. By
     * default we don't.
     */
    protected String getPromptContext ()
    {
        return null;
    }

    /**
     * Converts an exception returned by the server into a readable message.
     */
    protected String convertError (Throwable cause)
    {
        return CShell.serverError(cause);
    }

    protected void takeAction (boolean confirmed)
    {
        // if we have not yet confirmed and desire to do so, show the confirm popup
        if (getConfirmMessage() != null && !confirmed) {
            setEnabled(false);
            displayPopup();
            return;
        }

        // we're confirmed or don't want to, so go ahead and call the service
        if (callService()) {
            setEnabled(false);
        }
    }

    protected void displayPopup ()
    {
        new PromptPopup(getConfirmMessage(), null) {
            public void onAffirmative () {
                setEnabled(true);
                takeAction(true);
            }
            public void onNegative () {
                setEnabled(true);
            }
        }.setContext(getPromptContext()).prompt();
    }

    protected String getConfirmMessage ()
    {
        return _confirmMessage;
    }

    protected void setEnabled (boolean enabled)
    {
        if (_trigger instanceof FocusWidget) {
            ((FocusWidget)_trigger).setEnabled(enabled);

        } else if (_trigger instanceof Label) {
            Label tlabel = (Label)_trigger;
            tlabel.removeStyleName("actionLabel");
            if (enabled) {
                tlabel.addStyleName("actionLabel");
            }
        }

        // always remove first so that if we do end up adding, we don't doubly add
        _trigger.removeClickListener(_onClick);
        if (enabled) {
            _trigger.addClickListener(_onClick);
        }
    }

    protected Widget getErrorNear ()
    {
        return null;
    }

    protected ClickListener _onClick = new ClickListener() {
        public void onClick (Widget sender) {
            takeAction(false);
        }
    };

    protected SourcesClickEvents _trigger;
    protected String _confirmMessage;
}
