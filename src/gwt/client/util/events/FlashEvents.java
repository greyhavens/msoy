//
// $Id$

package client.util.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

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
        } else if (listener instanceof BackgroundChangeListener) {
            return BACKGROUND_CHANGED_EVENT;
        } else if (listener instanceof FurniChangeListener) {
            return FURNI_CHANGED_EVENT;
        } else if (listener instanceof LevelsListener) {
            return LEVEL_UPDATE_EVENT;
        } else if (listener instanceof PetListener) {
            return PET_EVENT;
        } else {
            return null;
        }
    }

    protected static FlashEvent eventForName (String eventName)
    {
        if (AVATAR_CHANGED_EVENT.equals(eventName)) {
            return new AvatarChangedEvent();
        } else if (BACKGROUND_CHANGED_EVENT.equals(eventName)) {
            return new BackgroundChangedEvent();
        } else if (FURNI_CHANGED_EVENT.equals(eventName)) {
            return new FurniChangedEvent();
        } else if (LEVEL_UPDATE_EVENT.equals(eventName)) {
            return new LevelUpdateEvent();
        } else if (PET_EVENT.equals(eventName)) {
            return new PetEvent();
        } else {
            return null;
        }
    }

    protected static native void configureEventCallback () /*-{
        $wnd.triggerFlashEvent = function (eventName, args) {
            @client.util.events.FlashEvents::triggerEvent(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(eventName, args);
        }
    }-*/;

    // defined in WorldClient.as
    protected static final String AVATAR_CHANGED_EVENT = "avatarChanged";

    // defined in RoomController.as
    protected static final String BACKGROUND_CHANGED_EVENT = "backgroundChanged";

    // defined in RoomController.as
    protected static final String FURNI_CHANGED_EVENT = "furniChanged";

    // defined in BaseClient.as
    protected static final String LEVEL_UPDATE_EVENT = "levelUpdate";

    // defined in RoomView.as
    protected static final String PET_EVENT = "pet";

    protected static Map _eventListeners = new HashMap();
}
