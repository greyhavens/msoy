//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.data.SpeakObject;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.group.data.all.GroupMembership;

public class PartyObject extends DObject
    implements SpeakObject, Cloneable
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>id</code> field. */
    public static final String ID = "id";

    /** The field name of the <code>name</code> field. */
    public static final String NAME = "name";

    /** The field name of the <code>icon</code> field. */
    public static final String ICON = "icon";

    /** The field name of the <code>group</code> field. */
    public static final String GROUP = "group";

    /** The field name of the <code>peeps</code> field. */
    public static final String PEEPS = "peeps";

    /** The field name of the <code>leaderId</code> field. */
    public static final String LEADER_ID = "leaderId";

    /** The field name of the <code>sceneId</code> field. */
    public static final String SCENE_ID = "sceneId";

    /** The field name of the <code>status</code> field. */
    public static final String STATUS = "status";

    /** The field name of the <code>recruitment</code> field. */
    public static final String RECRUITMENT = "recruitment";

    /** The field name of the <code>partyService</code> field. */
    public static final String PARTY_SERVICE = "partyService";

    /** The field name of the <code>speakService</code> field. */
    public static final String SPEAK_SERVICE = "speakService";
    // AUTO-GENERATED: FIELDS END

    /** This party's guid. */
    public int id;

    /** The name of this party. */
    public String name;

    /** The icon for this party. */
    public MediaDesc icon;

    /** The group under whose auspices we woop-it-up. */
    public GroupName group;

    /** The list of people in this party. */
    public DSet<PartyPeep> peeps = DSet.newDSet();

    /** The member ID of the current leader. */
    public int leaderId;

    /** The current location of the party. */
    public int sceneId;

    /** Customizable flavor text. */
    public String status;

    /** This party's access control. @see PartyCodes */
    public byte recruitment;

    /** The service for doing things on this party. */
    public PartyMarshaller partyService;

    /** Speaking on this party object. */
    public SpeakMarshaller speakService;

    // AUTO-GENERATED: METHODS START
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
     * Requests that the <code>icon</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setIcon (MediaDesc value)
    {
        MediaDesc ovalue = this.icon;
        requestAttributeChange(
            ICON, value, ovalue);
        this.icon = value;
    }

    /**
     * Requests that the <code>group</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGroup (GroupName value)
    {
        GroupName ovalue = this.group;
        requestAttributeChange(
            GROUP, value, ovalue);
        this.group = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>peeps</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPeeps (PartyPeep elem)
    {
        requestEntryAdd(PEEPS, peeps, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>peeps</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPeeps (Comparable<?> key)
    {
        requestEntryRemove(PEEPS, peeps, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>peeps</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePeeps (PartyPeep elem)
    {
        requestEntryUpdate(PEEPS, peeps, elem);
    }

    /**
     * Requests that the <code>peeps</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPeeps (DSet<PartyPeep> value)
    {
        requestAttributeChange(PEEPS, value, this.peeps);
        DSet<PartyPeep> clone = (value == null) ? null : value.typedClone();
        this.peeps = clone;
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
     * Requests that the <code>recruitment</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRecruitment (byte value)
    {
        byte ovalue = this.recruitment;
        requestAttributeChange(
            RECRUITMENT, Byte.valueOf(value), Byte.valueOf(ovalue));
        this.recruitment = value;
    }

    /**
     * Requests that the <code>partyService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPartyService (PartyMarshaller value)
    {
        PartyMarshaller ovalue = this.partyService;
        requestAttributeChange(
            PARTY_SERVICE, value, ovalue);
        this.partyService = value;
    }

    /**
     * Requests that the <code>speakService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setSpeakService (SpeakMarshaller value)
    {
        SpeakMarshaller ovalue = this.speakService;
        requestAttributeChange(
            SPEAK_SERVICE, value, ovalue);
        this.speakService = value;
    }
    // AUTO-GENERATED: METHODS END

    // from SpeakObject
    public void applyToListeners (ListenerOp op)
    {
        for (PartyPeep peep : peeps) {
            op.apply(peep.name);
        }
    }

    /**
     * May the specified player join this party? Note that you may join a party
     * you can't even see on the party board.
     *
     * @return the reason for failure, or null if joinage may proceed.
     */ 
    public String mayJoin (MemberName member, byte groupRank)
    {
        if (peeps.size() >= PartyCodes.MAX_PARTY_SIZE) {
            return PartyCodes.E_PARTY_FULL;
        }

        // TODO: invitation checking

        switch (recruitment) {
        case PartyCodes.RECRUITMENT_OPEN:
            return null;

        case PartyCodes.RECRUITMENT_GROUP:
            if (groupRank > GroupMembership.RANK_NON_MEMBER) {
                return null;
            }
            return InvocationCodes.E_ACCESS_DENIED;

        default:
        case PartyCodes.RECRUITMENT_CLOSED:
            return InvocationCodes.E_ACCESS_DENIED;
        }
    }

    @Override
    public Object clone ()
    {
        try {
            PartyObject that = (PartyObject)super.clone();
            that.peeps = this.peeps.typedClone();
            that.partyService = null;
            that.speakService = null;
            return that;

        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse);
        }
    }
}
