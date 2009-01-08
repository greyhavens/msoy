//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.presents.net.Credentials;

/**
 * Used to authenticate with the (logical) party server.
 */
public class PartyCredentials extends Credentials
{
    /** A session token that identifies this user. */
    public String sessionToken;

    /** The party that the authenticating user wishes to join. */
    public int partyId;
}
