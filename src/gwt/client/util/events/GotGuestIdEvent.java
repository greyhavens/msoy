//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.JavaScriptUtil;

/**
 * An event dispatched by the Flash client if the server assigns it a unique guest id. The GWT
 * client will store that id and use it later if the user opts to register and the server will then
 * use that guest id to transfer flow earned during their guest session to their newly created
 * account. Oh the twisty maze of passages.
 */
public class GotGuestIdEvent extends FlashEvent
{
    /** The name of this event type: defined in MsoyClient.as. */
    public static final String NAME = "gotGuestId";

    public GotGuestIdEvent ()
    {
    }

    public GotGuestIdEvent (int guestId)
    {
        _guestId = guestId;
    }

    public int getGuestId ()
    {
        return _guestId;
    }

    @Override // from FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    @Override // from FlashEvent
    public void fromJSObject (JavaScriptObject args)
    {
        _guestId = JavaScriptUtil.getIntElement(args, 0);
    }

    @Override // from FlashEvent
    public void toJSObject (JavaScriptObject args)
    {
        JavaScriptUtil.setIntElement(args, 0, _guestId);
    }

    @Override // from FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof GotGuestIdListener) {
            ((GotGuestIdListener)listener).gotGuestId(this);
        }
    }

    protected int _guestId;
}
