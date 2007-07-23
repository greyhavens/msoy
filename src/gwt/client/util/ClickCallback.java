//
// $Id$

package client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import client.shell.CShell;
import client.util.MsoyUI;

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
        _trigger = trigger;
        _trigger.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                if (callService()) {
                    _trigger.setEnabled(false);
                }
            }
        });
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

    protected Button _trigger;
}
