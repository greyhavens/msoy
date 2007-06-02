//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

/** 
 * Base class for all event listeners for incoming flash events. 
 */
public abstract class FlashEventListener
{
    /**
     * Supply the name that will be given by the flash client for this event.
     */
    public abstract String getEventName ();

    /**
     * parse out the arguments from the flash bridge, and trigger this event.
     */
    public abstract void trigger (JavaScriptObject arguments);
}
