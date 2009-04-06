//
// $Id$

package com.threerings.msoy.avrg.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.threerings.bureau.data.AgentObject;
import com.threerings.presents.dobj.DSet;
import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;

/**
 * The data shared between server and agent for an AVR game.
 */
public class AVRGameAgentObject extends AgentObject
    implements PropertySpaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>scenes</code> field. */
    public static final String SCENES = "scenes";

    /** The field name of the <code>gameOid</code> field. */
    public static final String GAME_OID = "gameOid";

    /** The field name of the <code>gameId</code> field. */
    public static final String GAME_ID = "gameId";

    /** The field name of the <code>agentService</code> field. */
    public static final String AGENT_SERVICE = "agentService";

    /** The field name of the <code>propertiesService</code> field. */
    public static final String PROPERTIES_SERVICE = "propertiesService";
    // AUTO-GENERATED: FIELDS END

    /** A set of scenes containing (or having recently contained) players of this AVRG. */
    public DSet<SceneInfo> scenes = new DSet<SceneInfo>();

    /** ID of the game object. */
    public int gameOid;

    /** ID of the game record. */
    public int gameId;

    /** Service for agent requests. */
    public AVRGameAgentMarshaller agentService;
    
    /** Used to set game properties. */
    public PropertySpaceMarshaller propertiesService;

    // from PropertySpaceObject
    public Map<String, Object> getUserProps ()
    {
        return _props;
    }

    // from PropertySpaceObject
    public Set<String> getDirtyProps ()
    {
        return _dirty;
    }

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
    public void removeFromScenes (Comparable<?> key)
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
        DSet<SceneInfo> clone = (value == null) ? null : value.typedClone();
        this.scenes = clone;
    }

    /**
     * Requests that the <code>gameOid</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGameOid (int value)
    {
        int ovalue = this.gameOid;
        requestAttributeChange(
            GAME_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.gameOid = value;
    }

    /**
     * Requests that the <code>gameId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGameId (int value)
    {
        int ovalue = this.gameId;
        requestAttributeChange(
            GAME_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.gameId = value;
    }

    /**
     * Requests that the <code>agentService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAgentService (AVRGameAgentMarshaller value)
    {
        AVRGameAgentMarshaller ovalue = this.agentService;
        requestAttributeChange(
            AGENT_SERVICE, value, ovalue);
        this.agentService = value;
    }

    /**
     * Requests that the <code>propertiesService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPropertiesService (PropertySpaceMarshaller value)
    {
        PropertySpaceMarshaller ovalue = this.propertiesService;
        requestAttributeChange(
            PROPERTIES_SERVICE, value, ovalue);
        this.propertiesService = value;
    }
    // AUTO-GENERATED: METHODS END
    
    /**
     * The current state of game data.
     * On the server, this will be a byte[] for normal properties and a byte[][] for array
     * properties. On the client, the actual values are kept whole.
     */
    protected transient HashMap<String, Object> _props = new HashMap<String, Object>();

    /**
     * The persistent properties that have been written to since startup.
     */
    protected transient Set<String> _dirty = new HashSet<String>();
}
