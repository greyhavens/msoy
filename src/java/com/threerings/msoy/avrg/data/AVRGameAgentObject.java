//
// $Id$

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

    /** The field name of the <code>gameOid</code> field. */
    public static final String GAME_OID = "gameOid";

    /** The field name of the <code>gameId</code> field. */
    public static final String GAME_ID = "gameId";

    /** The field name of the <code>agentService</code> field. */
    public static final String AGENT_SERVICE = "agentService";
    // AUTO-GENERATED: FIELDS END

    /** A set of scenes containing (or having recently contained) players of this AVRG. */
    public DSet<SceneInfo> scenes = new DSet<SceneInfo>();

    /** ID of the game object. */
    public int gameOid;

    /** ID of the game record. */
    public int gameId;

    /** Service for agent requests. */
    public AVRGameAgentMarshaller agentService;

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
    // AUTO-GENERATED: METHODS END
}
