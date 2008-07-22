//
// $Id$

package client.util.events;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

import com.threerings.gwt.util.ListenerList;

import client.shell.CShell;

/**
 * Utility class for listening to events from the Flash client.
 */
public class FlashEvents
{
    static {
        configureEventCallback();
    }

    /**
     * Registers an event listener to be notified when events arrive from the Flash client (or are
     * dispatched locally).
     */
    public static void addListener (FlashEventListener listener)
    {
        String name = nameForListener(listener);
        if (name != null) {
            ListenerList.addListener(_eventListeners, name, listener);
        }
    }

    /**
     * Clears out an event listener registration.
     */
    public static void removeListener (FlashEventListener listener)
    {
        String name = nameForListener(listener);
        if (name != null) {
            ListenerList.removeListener(_eventListeners, name, listener);
        }
    }

    /**
     * Dispatches an event to all registered listeners.
     */
    public static void dispatchEvent (final FlashEvent event)
    {
        ListenerList<FlashEventListener> listeners = _eventListeners.get(event.getEventName());
        if (listeners != null) {
            listeners.notify(new ListenerList.Op<FlashEventListener>() {
                public void notify (FlashEventListener listener) {
                    event.notifyListener(listener);
                }
            });
        }
    }

    /**
     * Called through the JavaScript bridge to dispatch an event that arrived from Flash.
     */
    protected static void triggerEvent (String eventName, JavaScriptObject args)
    {
        if (!_eventListeners.containsKey(eventName)) {
            return; // if no one is listening, stop here
        }
        FlashEvent event = eventForName(eventName);
        if (event != null) {
            event.readFlashArgs(args);
            dispatchEvent(event);
        }
    }

    protected static String nameForListener (FlashEventListener listener)
    {
        if (listener instanceof ItemUsageListener) {
            return ItemUsageEvent.NAME;
        } else if (listener instanceof StatusChangeListener) {
            return StatusChangeEvent.NAME;
        } else if (listener instanceof NameChangeListener) {
            return NameChangeEvent.NAME;
        } else if (listener instanceof FriendsListener) {
            return FriendEvent.NAME;
//        } else if (listener instanceof SceneBookmarkListener) {
//            return SceneBookmarkEvent.NAME;
        } else {
            CShell.log("Requested name for unknown listener '" + listener + "'?!");
            return null;
        }
    }

    protected static FlashEvent eventForName (String eventName)
    {
        if (ItemUsageEvent.NAME.equals(eventName)) {
            return new ItemUsageEvent();
        } else if (StatusChangeEvent.NAME.equals(eventName)) {
            return new StatusChangeEvent();
        } else if (NameChangeEvent.NAME.equals(eventName)) {
            return new NameChangeEvent();
        } else if (FriendEvent.NAME.equals(eventName)) {
            return new FriendEvent();
//        } else if (SceneBookmarkEvent.NAME.equals(eventName)) {
//            return new SceneBookmarkEvent();
        } else {
            CShell.log("Requested event for unknown name '" + eventName + "'?!");
            return null;
        }
    }

    protected static native void configureEventCallback () /*-{
        $wnd.triggerFlashEvent = function (eventName, args) {
            @client.util.events.FlashEvents::triggerEvent(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(eventName, args);
        }
    }-*/;

    protected static Map<String, ListenerList<FlashEventListener>> _eventListeners =
        new HashMap<String, ListenerList<FlashEventListener>>();
}
