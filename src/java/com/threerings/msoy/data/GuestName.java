//
// $Id$

package com.threerings.msoy.data;

import java.util.Arrays;

import com.threerings.util.Name;

/**
 * Used to track guests via a unique token for the duration of their session.
 */
public class GuestName extends Name
{
    /** The prefix for all authentication usernames provided to guests. */
    public static final String NAME_PREFIX = "Guest";

    // used for unserialization
    public GuestName ()
    {
    }

    public GuestName (byte[] sessionToken)
    {
        super(NAME_PREFIX + (++_guestCount));
        assert(sessionToken != null);
        _sessionToken = sessionToken;
    }

    // @Override // from Name
    public int hashCode ()
    {
        return Arrays.hashCode(_sessionToken);
    }

    // @Override // from Name
    public boolean equals (Object other)
    {
        if (other.getClass().equals(getClass())) {
            return Arrays.equals(_sessionToken, ((GuestName)other)._sessionToken);
        } else {
            return false;
        }
    }

    // @Override // from Name
    public int compareTo (Name other)
    {
        // guests sort lower than members
        if (!other.getClass().equals(getClass())) {
            return -1;
        }
        // if our session tokens are equal return 0, otherwise sort by name
        GuestName that = (GuestName)other;
        return Arrays.equals(_sessionToken, that._sessionToken) ? 0 : super.compareTo(other);
    }

    /** A unique token assigned to us at logon time. */
    protected byte[] _sessionToken;

    /** Used to assign unique usernames to guests that authenticate with the server. */
    protected static int _guestCount;
}
