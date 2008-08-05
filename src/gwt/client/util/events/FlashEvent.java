//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

public abstract class FlashEvent
{
    /**
     * Returns the string identifier for this event type.
     */
    public abstract String getEventName ();

    /**
     * Pull the expected values for this event out of the JavaScriptObject, using the utility
     * functions in FlashClients.
     */
    public abstract void fromJSObject (JavaScriptObject args);

    /**
     * Converts this object back into a JavaScriptObject, using the utility methods in FlashClients.
     */
    public abstract void toJSObject (JavaScriptObject args);

    /**
     * Events with the associated listener interface should implement this function and
     * notify the supplied listener if it implements their event listening interface.
     */
    public abstract void notifyListener (FlashEventListener listener);
}
