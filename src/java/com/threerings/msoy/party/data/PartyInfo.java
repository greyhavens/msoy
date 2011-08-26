//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

/**
 * Contains the mutable attributes of a party, published to the node objects and returned
 * as party of the party board.
 *
 * NOTE: please be careful about what fields you add. If fields are added that are needed by
 * one usage but not the other, we may need to consider having two different objects...
 */
@com.threerings.util.ActionScript(omit=true)
public class PartyInfo extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The unique party id. */
    public int id;

    /** The leader of this party. */
    public int leaderId;

    /** The status line indicating what this party is doing. */
    public String status;

    /** Helps interpret status with the right translation key (or none). */
    public byte statusType;

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
        int id, int leaderId, String status, byte statusType, int population, byte recruitment)
    {
        this.id = id;
        this.leaderId = leaderId;
        this.status = status;
        this.statusType = statusType;
        this.population = population;
        this.recruitment = recruitment;
    }

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return id;
    }
}
