//
// $Id: AVRGameObject.java 8844 2008-04-15 17:05:43Z nathan $

package com.threerings.msoy.avrg.data;

import com.threerings.bureau.data.AgentObject;
import com.threerings.presents.dobj.DSet;

/**
 * The data shared between server and agent for an AVR game.
 */
public class AVRGameAgentObject extends AgentObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>scenes</code> field. */
    public static final String SCENES = "scenes";
    // AUTO-GENERATED: FIELDS END

    /** A set of scenes containing (or having recently contained) players of this AVRG. */
    public DSet<SceneInfo> scenes = new DSet<SceneInfo>();
    
    /** ID of the game object. */
    public int gameOid;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>scenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToScenes (SceneInfo elem)
    {
        requestEntryAdd(SCENES, scenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>scenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromScenes (Comparable key)
    {
        requestEntryRemove(SCENES, scenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>scenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateScenes (SceneInfo elem)
    {
        requestEntryUpdate(SCENES, scenes, elem);
    }

    /**
     * Requests that the <code>scenes</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setScenes (DSet<SceneInfo> value)
    {
        requestAttributeChange(SCENES, value, this.scenes);
        @SuppressWarnings("unchecked") DSet<SceneInfo> clone =
            (value == null) ? null : value.typedClone();
        this.scenes = clone;
    }
    // AUTO-GENERATED: METHODS END
}
