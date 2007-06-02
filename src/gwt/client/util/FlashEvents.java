//
// $Id$

package client.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.events.FlashEventListener;

/**
 * Utility class for listening to events from the Flash client.
 */
public class FlashEvents
{ 
    static {
        configureEventCallback();
    }

    public static void addEventListener (FlashEventListener listener)
    {
        List listeners = (List)_eventListeners.get(listener.getEventName());
        if (listeners == null) {
            listeners = new ArrayList();
            _eventListeners.put(listener.getEventName(), listeners);
        }
        listeners.add(listener);
    }

    public static void removeEventListener (FlashEventListener listener)
    {
        List listeners = (List)_eventListeners.get(listener.getEventName());
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.size() == 0) {
                _eventListeners.remove(listener.getEventName());
            }
        }
    }

    protected static void triggerEvent (String eventName, JavaScriptObject args) 
    {
        List listeners = (List)_eventListeners.get(eventName);
        if (listeners != null) {
            Iterator iter = listeners.iterator();
            while (iter.hasNext()) {
                ((FlashEventListener)iter.next()).trigger(args);
            }
        }
    }

    protected static native void configureEventCallback () /*-{
        $wnd.triggerFlashEvent = function (eventName, args) {
            @client.util.FlashEvents::triggerEvent(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(eventName, args);
        }
    }-*/;

    protected static Map _eventListeners = new HashMap();
}
