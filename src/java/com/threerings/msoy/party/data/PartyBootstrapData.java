//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.presents.net.BootstrapData;

/**
 * Bootstrap data provided to a party client connection.
 */
@com.threerings.util.ActionScript(omit=true)
public class PartyBootstrapData extends BootstrapData
{
    /** The oid of the client's party object. */
    public int partyOid;
}
