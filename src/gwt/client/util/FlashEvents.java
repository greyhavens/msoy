//
// $Id$

package client.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.events.AvatarChangedEvent;
import client.util.events.AvatarChangeListener;
import client.util.events.FlashEventListener;
import client.util.events.FlashEvent;

/**
 * Utility class for listening to events from the Flash client.
 */
public class FlashEvents
{ 
    static {
        configureEventCallback();
    }

    public static void addListener (FlashEventListener listener)
    {
        String name = nameForListener(listener);
        if (name != null) {
            List listeners = (List)_eventListeners.get(name);
            if (listeners == null) {
                listeners = new ArrayList();
                _eventListeners.put(name, listeners);
            }
            listeners.add(listener);
        }
    }

    public static void removeListener (FlashEventListener listener)
    {
        String name = nameForListener(listener);
        if (name != null) {
            List listeners = (List)_eventListeners.get(name);
            if (listeners != null) {
                listeners.remove(listener);
            }
        }
    }

    protected static void triggerEvent (String eventName, JavaScriptObject args) 
    {
        List listeners = (List)_eventListeners.get(eventName);
        if (listeners != null) {
            FlashEvent event = eventForName(eventName);
            if (event != null) {
                event.readFlashArgs(args);
                Iterator iter = listeners.iterator();
                while (iter.hasNext()) {
                    event.notifyListener((FlashEventListener) iter.next());
                }
            }
        }
    }

    protected static String nameForListener (FlashEventListener listener)
    {
        if (listener instanceof AvatarChangeListener) {
            return AVATAR_CHANGED_EVENT;
        } else {
            return null;
        }
    }

    protected static FlashEvent eventForName (String eventName)
    {
        if (AVATAR_CHANGED_EVENT.equals(eventName)) {
            return new AvatarChangedEvent();
        } else {
            return null;
        }
    }

    protected static native void configureEventCallback () /*-{
        $wnd.triggerFlashEvent = function (eventName, args) {
            @client.util.FlashEvents::triggerEvent(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(eventName, args);
        }
    }-*/;

    // defined in WorldClient.as
    protected static final String AVATAR_CHANGED_EVENT = "avatarChanged";

    protected static Map _eventListeners = new HashMap();
}
