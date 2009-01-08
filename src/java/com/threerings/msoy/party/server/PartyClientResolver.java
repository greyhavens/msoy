//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.ClientResolver;

import com.threerings.msoy.party.data.PartierObject;

/**
 * Handles the resolution of partier client information.
 */
public class PartyClientResolver extends ClientResolver
{
    @Override // from PresentsClientResolver
    public ClientObject createClientObject ()
    {
        return new PartierObject();
    }

    @Override // from PresentsSession
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        PartierObject partObj = (PartierObject) clobj;

        // TBD
    }
}
