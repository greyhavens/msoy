//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.MemberObject;

/**
 * Summarized party info that is both published to the node objects and returned
 * to users as part of the party board.
 *
 * NOTE: please be careful about what fields you add. If fields are added that are needed by
 * one usage but not the other, we may need to consider having two different objects...
 */
public class PartyInfo extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The unique party id. */
    public int id;

    /** The name of this party. */
    public String name;

    /** The leader of this party. */
    public int leaderId;

    /** The group sponsoring this party. */
    public int groupId;

    /** The status line indicating what this party is doing. */
    public String status;

    /** The current population of this party. */
    public int population;

    /** The current recruitment status of this party. */
    public byte recruitment;

    /** Suitable for unserialization. */
    public PartyInfo ()
    {
    }

    /** Create a PartyInfo. */
    public PartyInfo (
        int id, String name, int leaderId, int groupId, String status,
        int population, byte recruitment)
    {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.groupId = groupId;
        this.status = status;
        this.population = population;
        this.recruitment = recruitment;
    }

    /**
     * Does this party appear on the partyBoard for the specified user?
     * Note that this just affects visibility, not whether the member may join.
     * Compare with PartyObject.mayJoin.
     */
    public boolean isVisible (MemberObject member)
    {
        if (population >= PartyCodes.MAX_PARTY_SIZE) {
            return false;
        }

        switch (recruitment) {
        case PartyCodes.RECRUITMENT_OPEN:
            return true;

        case PartyCodes.RECRUITMENT_GROUP:
            return member.isGroupMember(groupId);

        default:
        case PartyCodes.RECRUITMENT_CLOSED:
            return false;
        }
    }

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return id;
    }
}
