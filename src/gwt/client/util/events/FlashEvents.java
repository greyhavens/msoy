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
    /**
     * Registers an event listener to be notified when events arrive from the Flash client (or are
     * dispatched locally).
     */
    public static void addListener (FlashEventListener listener)
    {
        String name = nameForListener(listener);
        if (name != null) {
            ListenerList.addListener(getLLMap(), name, listener);
        }
    }

    /**
     * Clears out an event listener registration.
     */
    public static void removeListener (FlashEventListener listener)
    {
        String name = nameForListener(listener);
        if (name != null) {
            ListenerList.removeListener(getLLMap(), name, listener);
        }
    }

    /**
     * Creates an event object from the supplied Flash event data.
     */
    public static FlashEvent createEvent (String eventName, JavaScriptObject args)
    {
        FlashEvent event = eventForName(eventName);
        if (event != null) {
            event.fromJSObject(args);
        }
        return event;
    }

    /**
     * Internal method used by FrameEntryPoint or Page to dispatch an event to all registered
     * listeners. Don't use this method, use {@link Frame#dispatchEvent} which properly routes the 
     * event through the top-level frame and back down to the inner page. 
     */
    public static void internalDispatchEvent (final FlashEvent event)
    {
        ListenerList<FlashEventListener> listeners = getLLMap().get(event.getEventName());
        if (listeners != null) {
            listeners.notify(new ListenerList.Op<FlashEventListener>() {
                public void notify (FlashEventListener listener) {
                    event.notifyListener(listener);
                }
            });
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
        } else if (listener instanceof GotGuestIdListener) {
            return GotGuestIdEvent.NAME;
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
        } else if (GotGuestIdEvent.NAME.equals(eventName)) {
            return new GotGuestIdEvent();
        } else {
            CShell.log("Requested event for unknown name '" + eventName + "'?!");
            return null;
        }
    }

    /** We have to do this kookiness because methods in this class can be called from JSNI methods
     * which seem not to inoke the static initializers of the class before doing so. */
    protected static Map<String, ListenerList<FlashEventListener>> getLLMap () {
        if (_eventListeners == null) {
            _eventListeners = new HashMap<String, ListenerList<FlashEventListener>>();
        }
        return _eventListeners;
    }

    protected static Map<String, ListenerList<FlashEventListener>> _eventListeners;
}
