//
// $Id$

package client.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import client.shell.CShell;

/**
 * A handy class for dispatching notifications to listeners.
 */
public class ListenerList extends ArrayList
{
    /** Used by {@link ListenerList#notify}. */
    public static interface Op
    {
        /** Delivers a notification to the supplied listener. */
        public void notify (Object listener);
    }

    /**
     * Adds the supplied listener to the supplied list. If the list is null, a new listener list
     * will be created. The supplied or newly created list as appropriate will be returned.
     */
    public static ListenerList addListener (ListenerList list, Object listener)
    {
        if (list == null) {
            list = new ListenerList();
        }
        list.add(listener);
        return list;
    }

    /**
     * Adds a listener to the listener list in the supplied map. If no list exists, one will be
     * created and mapped to the supplied key.
     */
    public static void addListener (Map map, Object key, Object listener)
    {
        ListenerList list = (ListenerList)map.get(key);
        if (list == null) {
            map.put(key, list = new ListenerList());
        }
        list.add(listener);
    }

    /**
     * Removes a listener from the supplied list in the supplied map.
     */
    public static void removeListener (Map map, Object key, Object listener)
    {
        ListenerList list = (ListenerList)map.get(key);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Applies a notification to all listeners in this list.
     */
    public void notify (Op op)
    {
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Object listener = iter.next();
            try {
                op.notify(listener);
            } catch (Exception e) {
                CShell.log("Notification failed [list=" + this + ", op=" + op +
                           ", listener=" + listener + "].", e);
            }
        }
    }
}
