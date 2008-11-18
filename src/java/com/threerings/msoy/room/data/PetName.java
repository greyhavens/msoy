//
// $Id$

package com.threerings.msoy.room.data;

import com.samskivert.util.Comparators;

import com.threerings.util.Name;

/**
 * Uniquely identifies a pet, so that they may be muted.
 */
public class PetName extends Name
{
    /** For unserialization. */
    public PetName ()
    {
    }

    public PetName (String displayName, int petId)
    {
        super(displayName);
        _petId = petId;
    }

    @Override
    public int hashCode ()
    {
        return _petId;
    }

    @Override
    public boolean equals (Object other)
    {
        return (other instanceof PetName) && (((PetName) other)._petId == _petId);
    }

    @Override
    public int compareTo (Name o)
    {
        return Comparators.compare(_petId, ((PetName) o)._petId);
    }

    @Override
    protected String normalize (String name)
    {
        return name; // do not adjust
    }

    protected int _petId;
}
