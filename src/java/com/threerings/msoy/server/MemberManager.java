//
// $Id$

package com.threerings.msoy.server;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.server.persist.MemberRepository;

/**
 * Manage msoy members.
 */
public class MemberManager
    implements MemberProvider
{
    /** The member repository. */
    public MemberRepository memberRepo;

    /**
     * Construct our member manager.
     */
    public MemberManager (MemberRepository memberRepo)
    {
        this.memberRepo = memberRepo;
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
