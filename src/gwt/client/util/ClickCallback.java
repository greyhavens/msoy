//
// $Id$

package client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.WebContext;

/**
 * Allows one to wire up a button, a status label and a service call into one concisely specified
 * little chunk of code. Be sure to call <code>super.onSuccess()</code> and
 * <code>super.onFailure()</code> if you override those methods so that they can automatically
 * reenable the trigger button.
 */
public abstract class ClickCallback
    implements AsyncCallback
{
    /**
     * Creates a callback for the supplied trigger (the constructor will automatically add this
     * callback to the trigger as a click listener).
     */
    public ClickCallback (WebContext ctx, Button trigger)
    {
        this(ctx, trigger, null);
    }

    /**
     * Creates a callback for the supplied trigger (the constructor will automatically add this
     * callback to the trigger as a click listener). Failure will automatically be reported to the
     * supplied status label.
     */
    public ClickCallback (WebContext ctx, Button trigger, Label status)
    {
        _ctx = ctx;
        _trigger = trigger;
        _status = status;
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
        _trigger.setEnabled(true);
        if (_status != null) {
            _status.setText(cause.getMessage()); // TODO: decode and translate
        }
    }

    protected WebContext _ctx;
    protected Button _trigger;
    protected Label _status;
}
