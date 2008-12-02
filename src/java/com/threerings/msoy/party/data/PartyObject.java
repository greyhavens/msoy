//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.VizMemberName;

public class PartyObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>mates</code> field. */
    public static final String MATES = "mates";

    /** The field name of the <code>id</code> field. */
    public static final String ID = "id";

    /** The field name of the <code>leaderId</code> field. */
    public static final String LEADER_ID = "leaderId";

    /** The field name of the <code>sceneId</code> field. */
    public static final String SCENE_ID = "sceneId";

    /** The field name of the <code>status</code> field. */
    public static final String STATUS = "status";

    /** The field name of the <code>recruiting</code> field. */
    public static final String RECRUITING = "recruiting";
    // AUTO-GENERATED: FIELDS END

    /** The list of people in this party. */
    public DSet<VizMemberName> mates = new DSet<VizMemberName>();

    /** This party's guid. */
    public int id;

    /** The member ID of the current leader. */
    public int leaderId;

    /** The current location of the party. */
    public int sceneId;

    /** Customizable flavor text. */
    public String status;

    /** TODO: Doc. */
    public byte recruiting;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>mates</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMates (VizMemberName elem)
    {
        requestEntryAdd(MATES, mates, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>mates</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMates (Comparable<?> key)
    {
        requestEntryRemove(MATES, mates, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>mates</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMates (VizMemberName elem)
    {
        requestEntryUpdate(MATES, mates, elem);
    }

    /**
     * Requests that the <code>mates</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMates (DSet<VizMemberName> value)
    {
        requestAttributeChange(MATES, value, this.mates);
        DSet<VizMemberName> clone = (value == null) ? null : value.typedClone();
        this.mates = clone;
    }

    /**
     * Requests that the <code>id</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setId (int value)
    {
        int ovalue = this.id;
        requestAttributeChange(
            ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.id = value;
    }

    /**
     * Requests that the <code>leaderId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLeaderId (int value)
    {
        int ovalue = this.leaderId;
        requestAttributeChange(
            LEADER_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.leaderId = value;
    }

    /**
     * Requests that the <code>sceneId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setSceneId (int value)
    {
        int ovalue = this.sceneId;
        requestAttributeChange(
            SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.sceneId = value;
    }

    /**
     * Requests that the <code>status</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setStatus (String value)
    {
        String ovalue = this.status;
        requestAttributeChange(
            STATUS, value, ovalue);
        this.status = value;
    }

    /**
     * Requests that the <code>recruiting</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRecruiting (byte value)
    {
        byte ovalue = this.recruiting;
        requestAttributeChange(
            RECRUITING, Byte.valueOf(value), Byte.valueOf(ovalue));
        this.recruiting = value;
    }
    // AUTO-GENERATED: METHODS END
}
