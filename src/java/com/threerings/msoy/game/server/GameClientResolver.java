//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;

import com.threerings.presents.data.ClientObject;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.MemberCard;

import com.threerings.msoy.game.data.PlayerObject;

/**
 * Resolves an MSOY Game client's runtime data.
 */
public class GameClientResolver extends CrowdClientResolver
{
    @Override // from PresentsClientResolver
    public ClientObject createClientObject ()
    {
        return new PlayerObject();
    }

    @Override // from PresentsSession
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        PlayerObject playerObj = (PlayerObject) clobj;

        // guests have MemberName as an auth username, members have Name
        if (_username instanceof MemberName) {
            resolveGuest(playerObj);
        } else {
            resolveMember(playerObj);
        }
    }

    /**
     * Resolve a msoy member. This is called on the invoker thread.
     */
    protected void resolveMember (PlayerObject playerObj)
        throws Exception
    {
        // load up their member information using on their authentication (account) name
        MemberRecord member = _memberRepo.loadMember(_username.toString());

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening

        // we need their profile photo as well
        ProfileRecord precord = _profileRepo.loadProfile(member.memberId);
        playerObj.memberName = new VizMemberName(
            member.name, member.memberId,
            (precord == null) ? MemberCard.DEFAULT_PHOTO : precord.getPhoto());

        // configure various bits directly from their member record
        playerObj.humanity = member.humanity;

        // for players, resolve this here from the database.
        // guests will get resolution later on, in GameSession.sessionWillStart()
        playerObj.visitorInfo = new VisitorInfo(member.visitorId, true);
    }

    /**
     * Resolve a lowly guest. This is called on the invoker thread.
     */
    protected void resolveGuest (PlayerObject playerObj)
        throws Exception
    {
        // our auth username has our assigned name and member id, so use those
        MemberName aname = (MemberName)_username;
        playerObj.memberName = new VizMemberName(
            aname.toString(), aname.getMemberId(), MemberCard.DEFAULT_PHOTO);

        // guests operate at the default new user humanity level
        playerObj.humanity = MsoyCodes.STARTING_HUMANITY;
    }

    // our dependencies
    @Inject protected MemberRepository _memberRepo;
    @Inject protected ProfileRepository _profileRepo;
}
