//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.all.GroupName;

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

    /** The group sponsoring this party. */
    public GroupName group;

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
        int id, String name, GroupName group, String status, int population, byte recruitment)
    {
        this.id = id;
        this.name = name;
        this.group = group;
        this.status = status;
        this.population = population;
        this.recruitment = recruitment;
    }
            
    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return id;
    }
}
