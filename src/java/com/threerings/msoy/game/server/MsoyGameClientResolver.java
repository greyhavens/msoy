//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.presents.data.ClientObject;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.server.MsoyObjectAccess;
import com.threerings.msoy.server.persist.MemberRecord;

/**
 * Resolves an MSOY Game client's runtime data.
 */
public class MsoyGameClientResolver extends CrowdClientResolver
{
    @Override // from PresentsClientResolver
    public ClientObject createClientObject ()
    {
        return new PlayerObject();
    }

    @Override // from PresentsClient
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        PlayerObject userObj = (PlayerObject) clobj;
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
    protected void resolveMember (PlayerObject userObj)
        throws Exception
    {
        // load up their member information using on their authentication (account) name
        MemberRecord member = MsoyGameServer.memberRepo.loadMember(_username.toString());

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening

        // configure various bits directly from their member record
        userObj.memberName = member.getName();
//         userObj.flow = member.flow;
//         userObj.accFlow = member.accFlow;
//         userObj.level = member.level;
//         userObj.humanity = member.humanity;

        // load up their selected avatar, we'll configure it later
// TODO
//         if (member.avatarId != 0) {
//             AvatarRecord avatar =
//                 MsoyServer.itemMan.getAvatarRepository().loadItem(member.avatarId);
//             if (avatar != null) {
//                 userObj.avatar = (Avatar)avatar.toItem();
//             }
//         }
    }

    /**
     * Resolve a lowly guest. This is called on the invoker thread.
     */
    protected void resolveGuest (PlayerObject userObj)
        throws Exception
    {
        userObj.memberName = (MemberName)_username;
    }

    /**
     * Return true if we're resolving a guest.
     */
    protected boolean isResolvingGuest ()
    {
        // this seems strange, but we're testing the authentication username, which is set to be a
        // MemberName for guests and a regular Name for members. The reason for this is that the
        // guests will use the same MemberName object for their display name and auth name
        return (_username instanceof MemberName);
    }
}
