//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.msoy.data.AuthName;

/**
 * Identifies the auth-username of a party authentication request.
 */
public class PartyAuthName extends AuthName
{
    /** Creates a name for the member with the supplied account name and member id. */
    public PartyAuthName (String accountName, int memberId)
    {
        super(accountName, memberId);
    }

    /** Used for unserializing. */
    public PartyAuthName ()
    {
    }
}
