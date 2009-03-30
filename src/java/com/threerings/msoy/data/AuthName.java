//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.Comparators;
import com.threerings.util.Name;

/**
 * Represents the authentication username for our various sessions (world, game, party).
 */
public class AuthName extends Name
{
    /** Creates a name for the member with the supplied account name and member id. */
    public AuthName (String accountName, int memberId)
    {
        super(accountName);
        _memberId = memberId;
    }

    /** Used when unserializing. */
    public AuthName ()
    {
    }

    /** Returns this member's unique id. */
    public int getMemberId ()
    {
        return _memberId;
    }

    @Override // from Name
    public int hashCode ()
    {
        return _memberId;
    }

    @Override // from Name
    public boolean equals (Object other)
    {
        return (other != null) && other.getClass().equals(getClass()) &&
            (((AuthName)other).getMemberId() == getMemberId());
    }

    @Override // from Name
    public int compareTo (Name o)
    {
        int rv = getClass().getName().compareTo(o.getClass().getName());
        return (rv != 0) ? rv : Comparators.compare(getMemberId(), ((AuthName)o).getMemberId());
    }

    protected int _memberId;
}
