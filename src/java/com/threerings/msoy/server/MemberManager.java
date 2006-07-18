//
// $Id$

package com.threerings.msoy.server;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

/**
 * Manage msoy members.
 */
public class MemberManager
    implements MemberProvider
{
    /**
     * Construct our member manager.
     */
    public MemberManager ()
    {
        MsoyServer.invmgr.registerDispatcher(new MemberDispatcher(this), true);
    }

    // from interface MemberProvider
    public void alterFriend (
            ClientObject caller, Name friend, boolean add,
            InvocationService.InvocationListener listener)
        throws InvocationException
    {
        throw new InvocationException("TODO");
    }
}
