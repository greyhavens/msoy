//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.OccupantInfo;

import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.party.data.PartyLeader;
import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;

/**
 * Extends Whirled game stuff with party awareness.
 */
public class ParlorGameObject extends WhirledGameObject
    implements PartyPlaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>name</code> field. */
    public static final String NAME = "name";

    /** The field name of the <code>parties</code> field. */
    public static final String PARTIES = "parties";

    /** The field name of the <code>partyLeaders</code> field. */
    public static final String PARTY_LEADERS = "partyLeaders";
    // AUTO-GENERATED: FIELDS END

    /** The name of this game. */
    public String name;

    /** Information on the parties presently in this game. */
    public DSet<PartySummary> parties = DSet.newDSet();

    /** Current party leaders. */
    public DSet<PartyLeader> partyLeaders = DSet.newDSet();

    // from PartyPlaceObject
    public DSet<PartySummary> getParties ()
    {
        return parties;
    }

    // from PartyPlaceObject
    public DSet<OccupantInfo> getOccupants ()
    {
        return occupantInfo;
    }

    // from PartyPlaceObject
    public DSet<PartyLeader> getPartyLeaders ()
    {
        return partyLeaders;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>name</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setName (String value)
    {
        String ovalue = this.name;
        requestAttributeChange(
            NAME, value, ovalue);
        this.name = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>parties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToParties (PartySummary elem)
    {
        requestEntryAdd(PARTIES, parties, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>parties</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromParties (Comparable<?> key)
    {
        requestEntryRemove(PARTIES, parties, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>parties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateParties (PartySummary elem)
    {
        requestEntryUpdate(PARTIES, parties, elem);
    }

    /**
     * Requests that the <code>parties</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setParties (DSet<PartySummary> value)
    {
        requestAttributeChange(PARTIES, value, this.parties);
        DSet<PartySummary> clone = (value == null) ? null : value.typedClone();
        this.parties = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>partyLeaders</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPartyLeaders (PartyLeader elem)
    {
        requestEntryAdd(PARTY_LEADERS, partyLeaders, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>partyLeaders</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPartyLeaders (Comparable<?> key)
    {
        requestEntryRemove(PARTY_LEADERS, partyLeaders, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>partyLeaders</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePartyLeaders (PartyLeader elem)
    {
        requestEntryUpdate(PARTY_LEADERS, partyLeaders, elem);
    }

    /**
     * Requests that the <code>partyLeaders</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPartyLeaders (DSet<PartyLeader> value)
    {
        requestAttributeChange(PARTY_LEADERS, value, this.partyLeaders);
        DSet<PartyLeader> clone = (value == null) ? null : value.typedClone();
        this.partyLeaders = clone;
    }
    // AUTO-GENERATED: METHODS END
}
