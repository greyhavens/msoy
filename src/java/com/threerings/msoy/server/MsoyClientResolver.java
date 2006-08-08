//
// $Id$

package com.threerings.msoy.server;

import java.util.ArrayList;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MemberName;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.server.persist.Member;

/**
 * Used to configure msoy-specific client object data.
 */
public class MsoyClientResolver extends CrowdClientResolver
{
    @Override
    public Class<? extends ClientObject> getClientObjectClass ()
    {
        return MemberObject.class;
    }

    /**
     * Return true if we're resolving a guest.
     */
    protected boolean isResolvingGuest ()
    {
        return !(_username instanceof MemberName) ||
            (((MemberName) _username).getMemberId() == -1);
    }

    @Override
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        MemberObject userObj = (MemberObject) clobj;
        if (isResolvingGuest()) {
            resolveGuest(userObj);

        } else {
            resolveMember(userObj);
        }
    }

    /**
     * Resolve a msoy member. This is called on the invoker thread.
     */
    protected void resolveMember (MemberObject userObj)
        throws Exception
    {
        // TODO
        userObj.setTokens(new MsoyTokenRing());

        // TODO: use the real account name to load stuff
        Member member = MsoyServer.memberRepo.loadMember(
            userObj.username.toString());

        userObj.setMemberId(member.memberId);
        // TODO: etc..

        // load friends
        ArrayList<FriendEntry> friends =
            MsoyServer.memberRepo.getFriends(member.memberId);
        userObj.setFriends(new DSet<FriendEntry>(friends.iterator()));
    }

    /**
     * Resolve a lowly guest. This is called on the invoker thread.
     */
    protected void resolveGuest (MemberObject userObj)
        throws Exception
    {
        userObj.setTokens(new MsoyTokenRing());
    }
}
