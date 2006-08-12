//
// $Id$

package com.threerings.msoy.server;

import java.util.ArrayList;

import com.samskivert.util.ResultListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MemberName;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.server.persist.Member;

import static com.threerings.msoy.Log.log;

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

    @Override // from PresentsClient
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        MemberObject userObj = (MemberObject) clobj;
        // set up the standard user access controller
        userObj.setAccessController(MsoyObjectAccess.USER);
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
        // load up their member information using on their authentication
        // (account) name
        Member member = MsoyServer.memberRepo.loadMember(_username.toString());

        // configure their member name which is a combination of their display
        // name and their member id
        userObj.setMemberName(new MemberName(member.name, member.memberId));

        // TODO
        userObj.setTokens(new MsoyTokenRing());
        // TODO: etc..
    }

    /**
     * Resolve a lowly guest. This is called on the invoker thread.
     */
    protected void resolveGuest (MemberObject userObj)
        throws Exception
    {
        userObj.setTokens(new MsoyTokenRing());
        // TODO: make a proper guest display name
        userObj.setMemberName(new MemberName(userObj.username.toString(), -1));
    }

    @Override // from PresentsClient
    protected void finishResolution (ClientObject clobj)
    {
        super.finishResolution(clobj);

        final MemberObject user = (MemberObject)clobj;

        // load up their friend info
        if (!user.isGuest()) {
            MsoyServer.memberMan.loadFriends(user.getMemberId(),
                new ResultListener<ArrayList<FriendEntry>>() {
                public void requestCompleted (ArrayList<FriendEntry> friends) {
                    user.setFriends(
                        new DSet<FriendEntry>(friends.iterator()));
                }
                public void requestFailed (Exception cause) {
                    log.warning("Failed to load member's friend info " +
                        "[who=" + user.who() + ", error=" + cause + "].");
                }
            });
        }
    }

    /**
     * Return true if we're resolving a guest.
     */
    protected boolean isResolvingGuest ()
    {
        return _username.toString().startsWith(
            MsoyClient.GUEST_USERNAME_PREFIX);
    }
}
