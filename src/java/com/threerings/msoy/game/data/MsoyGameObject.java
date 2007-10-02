//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.ezgame.data.EZGameObject;

import com.whirled.data.GameData;
import com.whirled.data.Ownership;
import com.whirled.data.WhirledGame;
import com.whirled.data.WhirledGameMarshaller;

/**
 * Maintains additional state for MSOY games.
 */
public class MsoyGameObject extends EZGameObject
    implements WhirledGame
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>whirledGameService</code> field. */
    public static final String WHIRLED_GAME_SERVICE = "whirledGameService";

    /** The field name of the <code>gameData</code> field. */
    public static final String GAME_DATA = "gameData";

    /** The field name of the <code>ownershipData</code> field. */
    public static final String OWNERSHIP_DATA = "ownershipData";
    // AUTO-GENERATED: FIELDS END

    /** The whirled game services. */
    public WhirledGameMarshaller whirledGameService;

    /** The various game data available to this game. */
    public GameData[] gameData;

    /** Contains info on which player owns which game data. */
    public DSet<Ownership> ownershipData;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>whirledGameService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setWhirledGameService (WhirledGameMarshaller value)
    {
        WhirledGameMarshaller ovalue = this.whirledGameService;
        requestAttributeChange(
            WHIRLED_GAME_SERVICE, value, ovalue);
        this.whirledGameService = value;
    }

    /**
     * Requests that the <code>gameData</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGameData (GameData[] value)
    {
        GameData[] ovalue = this.gameData;
        requestAttributeChange(
            GAME_DATA, value, ovalue);
        this.gameData = (value == null) ? null : (GameData[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>gameData</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setGameDataAt (GameData value, int index)
    {
        GameData ovalue = this.gameData[index];
        requestElementUpdate(
            GAME_DATA, index, value, ovalue);
        this.gameData[index] = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>ownershipData</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToOwnershipData (Ownership elem)
    {
        requestEntryAdd(OWNERSHIP_DATA, ownershipData, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>ownershipData</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromOwnershipData (Comparable key)
    {
        requestEntryRemove(OWNERSHIP_DATA, ownershipData, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>ownershipData</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateOwnershipData (Ownership elem)
    {
        requestEntryUpdate(OWNERSHIP_DATA, ownershipData, elem);
    }

    /**
     * Requests that the <code>ownershipData</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setOwnershipData (DSet<com.whirled.data.Ownership> value)
    {
        requestAttributeChange(OWNERSHIP_DATA, value, this.ownershipData);
        @SuppressWarnings("unchecked") DSet<com.whirled.data.Ownership> clone =
            (value == null) ? null : value.typedClone();
        this.ownershipData = clone;
    }
    // AUTO-GENERATED: METHODS END
}
