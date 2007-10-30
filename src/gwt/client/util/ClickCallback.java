//
// $Id$

package client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import client.shell.CShell;

/**
 * Allows one to wire up a button and a service call into one concisely specified little chunk of
 * code. Be sure to call <code>super.onSuccess()</code> and <code>super.onFailure()</code> if you
 * override those methods so that they can automatically reenable the trigger button.
 */
public abstract class ClickCallback
    implements AsyncCallback
{
    /**
     * Creates a callback for the supplied trigger (the constructor will automatically add this
     * callback to the trigger as a click listener). Failure will automatically be reported.
     */
    public ClickCallback (Button trigger)
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
    public ClickCallback (Button trigger, String confirmMessage)
    {
        _trigger = trigger;
        _trigger.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                takeAction(false);
            }
        });
        _confirmMessage = confirmMessage;
    }

    /**
     * This method is called when the trigger button is clicked. Pass <code>this</code> as the
     * {@link AsyncCallback} to a service method. Return true from this method if a service request
     * was initiated and the button that triggered it should be disabled.
     */
    public abstract boolean callService ();

    /**
     * This method will be called when the service returns successfully. Return true if the trigger
     * should now be reenabled, false to leave it disabled.
     */
    public abstract boolean gotResult (Object result);

    // from interface AsyncCallback
    public void onSuccess (Object result)
    {
        _trigger.setEnabled(gotResult(result));
    }

    // from interface AsyncCallback
    public void onFailure (Throwable cause)
    {
        CShell.log("Callback failure [for=" + _trigger.getText() + "]", cause);
        _trigger.setEnabled(true);
        MsoyUI.error(CShell.serverError(cause));
    }

    protected void takeAction (boolean confirmed)
    {
        // if we have not yet confirmed and desire to do so, show the confirm popup
        if (_confirmMessage != null && !confirmed) {
            _trigger.setEnabled(false);
            new PromptPopup(_confirmMessage) {
                public void onAffirmative () {
                    _trigger.setEnabled(true);
                    takeAction(true);
                }
                public void onNegative () {
                    _trigger.setEnabled(true);
                }
            }.prompt();
            return;
        }

        // we're confirmed or don't want to, so go ahead and call the service
        if (callService()) {
            _trigger.setEnabled(false);
        }
    }

    protected Button _trigger;
    protected String _confirmMessage;
}
