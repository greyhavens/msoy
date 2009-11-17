//
// $Id$

package client.util.events;

import client.util.JavaScriptUtil;

import com.google.gwt.core.client.JavaScriptObject;
import com.threerings.msoy.web.gwt.Pages;

/**
 * An event for flash to send a string command to a GWT page.
 */
public class PageCommandEvent extends FlashEvent
{
    /** The name of this event type, also defined in action script. */
    public static final String NAME = "pageCommand";

    /** Command for showing the interface for editing a profile. */
    public static final String EDIT_PROFILE = "editProfile";

    /** Implemented by entities which wish to act upon commands from flash. */
    public static interface Listener extends FlashEventListener
    {
        /**
         * Notifies the listener that a command event was received from the flash client. The
         * listener should return true if the event was consumed, i.e. some action was taken.
         */
        public boolean act (PageCommandEvent commandEvent);
    }

    public PageCommandEvent ()
    {
    }

    public PageCommandEvent (Pages page, String command)
    {
        _target = page;
        _command = command;
    }

    public Pages getTarget ()
    {
        return _target;
    }

    public String getCommand ()
    {
        return _command;
    }
    
    @Override // from FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    @Override // from FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof Listener) {
            ((Listener)listener).act(this);
        }
    }

    @Override // from FlashEvent
    public void fromJSObject (JavaScriptObject args)
    {
        _target = Pages.valueOf(JavaScriptUtil.getStringElement(args, 0));
        _command = JavaScriptUtil.getStringElement(args, 1);
    }

    @Override // from FlashEvent
    public void toJSObject (JavaScriptObject args)
    {
        JavaScriptUtil.setStringElement(args, 0, _target.toString());
        JavaScriptUtil.setStringElement(args, 1, _command);
    }

    protected Pages _target;
    protected String _command;
}

